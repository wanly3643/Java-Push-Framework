package org.push.core;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.push.core.Common.SendResult;
import org.push.monitor.MonitorsMsgFactory;
import org.push.protocol.Buffer;
import org.push.protocol.Connection;
import org.push.protocol.ErrorCodes.NetworkSerializeResult;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolManager;
import org.push.protocol.RecyclableBuffer;
import org.push.protocol.SerializeData;
import org.push.util.CppEnum;
import org.push.util.Debug;
import org.push.util.Ptr;
import org.push.util.Utils;

/**
 * A wrapper to manage read/write data to the socket.
 * 
 * @author Lei Wang
 */

public class PhysicalConnection extends Connection {

	public enum Status implements CppEnum {
        Disposable(0),
        WaitingForWrite(1),
        Connected(2),
        Attached(3);

        private int value;

        private Status(int value) { this.value = value; }

        public int value() { return this.value; }
    };
    
    private boolean bIsObserver;
    private Status status;//status
    private AtomicLong ioWorkersReferenceCounter = new AtomicLong(0);
    private long dtCreationTime;
    private long dtLastReceiveTime;
    private Lock csLock;
    private LogicalConnectionImpl logicalConnection;
    private RecyclableBuffer oBuffer;
    private volatile boolean bWriteInProgress;

    private PushClientSocket socket;
    private int rPeerPort;
    private String rPeerIP;
    private ListenerOptions listenerOptions;
    private ConnectionContext connectionContext;
    //private boolean isInitialized;
    
    //
    private ServerImpl serverImpl;

	public PhysicalConnection(ServerImpl serverImpl) {
		oBuffer = new RecyclableBuffer(RecyclableBuffer.Type.Multiple);
		listenerOptions = null;
		connectionContext = null;

		csLock = new ReentrantLock();
		
		this.serverImpl = serverImpl;
	}
	
	/**
	 * Write the data into the socket
	 * 
	 * @return true if succeed
	 */
    private boolean WriteBytes() {
        //We should call send, until the return value is EAGAIN
	    ServerStats stats = serverImpl.getServerStats();
	    ServerOptions options = serverImpl.getServerOptions();
        while (true) {
        	// How many bytes to send
            int dwToPick = Math.min(options.getSocketBufferSize(), 
            		oBuffer.getDataSize());
            if (dwToPick == 0) {
                return true;
            }

            try {
            	int ret = socket.send(oBuffer.getBuffer(), 0, dwToPick);
            	
            	if (ret > 0) {
            		// Remove the data already sent
            		oBuffer.pop(ret);

                    if (!isObserverChannel()) {
                        stats.addToCumul(ServerStats.Measures.BandwidthOutbound, 
                        		dwToPick);
                        stats.addToKeyedDuration(
                        		ServerStats.Measures.BandwidthOutboundPerConnection, 
                        		this.getSocket().hashCode(), dwToPick);
                    }
            	} else {
            		return false;
            	}
            } catch (IOException ioe) {
            	return false;
            }
        }
    }

    private void CloseSocket() {
    	socket.close();
    	status = Status.Disposable;
    }

    public void closeConnection(boolean bWaitForSendsToComplete) {
    	csLock.lock();

        if (status.value() < Status.Connected.value()){
            csLock.unlock();
            return;
        }
    	
    	//Either connected or attached :
        if (bWaitForSendsToComplete) {
            if (oBuffer.getDataSize() == 0) {
                CloseSocket();
            } else {
                status = Status.WaitingForWrite;
            }
        } else {
            CloseSocket();
        }
        
        // Remove from the queue
        serverImpl.getIOQueue().deleteSocketContext(socket);
        
        Debug.debug("Connection closed: " + rPeerIP + ":" + rPeerPort);
        
        csLock.unlock();
    }

    public void postReceive() {
    	if (status.value() < Status.Connected.value()){
    		return;
    	}
    	
    	if(serverImpl.getIOQueue().rearmSocketForRead(getSocket(), this))
    	{
    		incrementIoWorkersReferenceCounter();
    	}
    }

    public boolean readReceivedBytes(RecyclableBuffer incomingBytes, 
    		int dwIoSize) {
        incomingBytes.doAllocate(RecyclableBuffer.Type.Socket);
        
        while (true) {
    		if (incomingBytes.isFull()) {
    			return true;
    		}

    		try {
    			int ret = socket.recv(incomingBytes.getBuffer(), 
    					incomingBytes.getDataSize(),
    					incomingBytes.getRemainingSize());
    			
    			if (ret < 0) {
    				return false;
    			}
    			
    			if (ret == 0) {
    				break;
    			}
    			
    			Debug.debug(ret + " bytes read from " + rPeerIP + ":" + 
    					rPeerPort);

                incomingBytes.growSize(ret);
                dtLastReceiveTime = System.currentTimeMillis();
    		} catch (IOException ioe) {
    			ioe.printStackTrace();
    			return false;
    		}
        }

    	// Report bytes read to stats object.
    	if (!isObserverChannel()) {
    	    ServerStats stats = serverImpl.getServerStats();
    		int nBytes = incomingBytes.getDataSize();
    		stats.addToCumul(ServerStats.Measures.BandwidthInbound, nBytes);
    		stats.addToKeyedDuration(
    				ServerStats.Measures.BandwidthInboundPerConnection, 
    				this.getSocket().hashCode(), nBytes);
    	}

        return socket.isConnected();
    }

    public SendResult pushPacket(OutgoingPacket packet) {
    	csLock.lock();
    	SendResult ret = pushPacketCommon(packet);
    	csLock.unlock();
    	return ret;
    }

    public SendResult pushPacketCommon(OutgoingPacket packet) {
        if (status.value() < Status.Connected.value()) {
    		return SendResult.NotOK;
        }
	    Dispatcher dispatcher = serverImpl.getDispatcher();
	    ProtocolManager theProtocolManager = serverImpl.getProtocolManager();
	    ServerStats stats = serverImpl.getServerStats();

    	int nBytesWritten = oBuffer.getDataSize();

    	SerializeData serializeData = new SerializeData(getProtocol());
    	NetworkSerializeResult result = 
    		theProtocolManager.serializeOutgoingPacket(this, packet, oBuffer, 
    				serializeData);

        if (result != NetworkSerializeResult.Success) {
    		return result == NetworkSerializeResult.Retry ? 
    				SendResult.Retry : SendResult.NotOK;
        }

    	nBytesWritten = oBuffer.getDataSize() - nBytesWritten;
        stats.addToCumul(ServerStats.Measures.BandwidthOutstanding, 
        		nBytesWritten);
        String serviceName;
        if ((serviceName = dispatcher.getCurrentService()) != null) {
            stats.addToDistribution(
            		ServerStats.Measures.BandwidthOutboundVolPerRequest, 
            		serviceName, nBytesWritten);
        }

        if (!bWriteInProgress) {
    		return WriteBytes() ? SendResult.OK : SendResult.NotOK;
        }
    	return SendResult.OK;
    }

    public SendResult pushBytes(Buffer buffer, Protocol protocol) {
    	// This is called by protocol. There is no need to do statistics.
    	if (status.value() < Status.Connected.value()) {
    		return SendResult.NotOK;
    	}
	    ProtocolManager theProtocolManager = serverImpl.getProtocolManager();

    	SerializeData serializeData = new SerializeData(protocol);
    	NetworkSerializeResult result = 
    		theProtocolManager.serializeOutgoinBytes(
    				this, buffer, oBuffer, serializeData);
    	
    	if (result != NetworkSerializeResult.Success) {
    		return result == NetworkSerializeResult.Retry ? 
    				SendResult.Retry : SendResult.NotOK;
    	}

    	if (!bWriteInProgress) {
    		return WriteBytes() ? SendResult.OK : SendResult.NotOK;
    	}

    	return SendResult.OK;
    }

    public SendResult tryPushPacket(OutgoingPacket packet) {
        if (csLock.tryLock()) {
            SendResult result = pushPacketCommon(packet);
            csLock.unlock();
            return result;
        }
    	return SendResult.Retry;
    }

    public boolean onSendCompleted(Ptr<Boolean> pIsBufferIdle) {
    	csLock.lock();

    	if(status.value() < Status.WaitingForWrite.value()) {
        	csLock.unlock();
    		return false;
    	}

        bWriteInProgress = false;

    	if(!WriteBytes()) {
        	csLock.unlock();
    		return false;
    	}

        boolean bIsBufferIdle = 
        	(oBuffer.getDataSize() == 0 && !bWriteInProgress);
        pIsBufferIdle.set(new Boolean(bIsBufferIdle));
        if(bIsBufferIdle && status == Status.WaitingForWrite) {
    		CloseSocket();
        	csLock.unlock();
            return false;
    	}
    	csLock.unlock();
    	
    	return true;
    }

    public Buffer getSendBuffer() { return oBuffer; }

    public boolean isWriteInProgress() { return  bWriteInProgress; }

    public void reset(PushClientSocket socket, boolean bIsObserver, 
    		ListenerOptions listenerOptions) {
    	this.socket = socket;

    	this.rPeerPort = socket.getPort();
    	this.rPeerIP = socket.getIP();

    	this.listenerOptions = listenerOptions;

    	this.dtCreationTime = System.currentTimeMillis();

    	this.bWriteInProgress = false;

    	this.bIsObserver = bIsObserver;

    	this.status = Status.Connected;

    	this.ioWorkersReferenceCounter = new AtomicLong(0);
    }

    public void recycle() {
    	oBuffer.clearBytes();

    	if (connectionContext != null) {
    	    ConnectionContextPool connectionContextPool = 
    	    	serverImpl.getConnectionContextPool();
    		connectionContextPool.returnObject(connectionContext);
    		connectionContext = null;
    	}

    	super.recycle();

    	listenerOptions = null;
    }
	
    public Status getStatus() { return status; }

    public boolean isObserverChannel() { return bIsObserver; }

    public boolean checkIfUnusedByIOWorkers() {
    	return status == Status.Disposable && 
    		ioWorkersReferenceCounter.longValue() == 0;
    }

    public long getIoWorkersReferenceCounter() {
    	return ioWorkersReferenceCounter.get();
    }

    public void incrementIoWorkersReferenceCounter() {
    	ioWorkersReferenceCounter.incrementAndGet();
    }

    public void decrementIoWorkersReferenceCounter() {
    	ioWorkersReferenceCounter.decrementAndGet();
    }

    public int getPeerPort() { return rPeerPort; }
    public String getPeerIP() { return rPeerIP; }

    public double getLifeDuration() {
        return Utils.timePassed(dtCreationTime);
    }
    public double getTimeToLastReceive() {
        return Utils.timePassed(dtLastReceiveTime);
    }

    public void setConnectionContext(ConnectionContext pConnectionContext) {
        this.connectionContext = pConnectionContext;
    }

    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    public void attachToClient(LogicalConnectionImpl logicalConnection) {
        this.logicalConnection = logicalConnection;
        status = Status.Attached;
    }

    public LogicalConnectionImpl getLogicalConnectionImpl() {
        return this.logicalConnection;
    }

    public PushClientSocket getSocket() { return socket; }


	public Protocol getProtocol() {
		return listenerOptions.getProtocol();
	}

	@Override
	public MessageFactory getMessageFactory() {
		return isObserverChannel() ? MonitorsMsgFactory.getDefault() : 
			serverImpl.getMessageFactory();
	}

	@Override
	protected void initializeConnection() {
		if (isObserverChannel()) {
			return;
		}

		if (serverImpl.getServerOptions().challengeClients()) {
			connectionContext = 
				serverImpl.getConnectionContextPool().borrowObject();
		}

		OutgoingPacket msg = serverImpl.getFacade().getChallenge(
				connectionContext);

		if (msg != null) {
			pushPacket(msg);
			// We call the message factory of clients not monitors.
			serverImpl.getMessageFactory().disposeOutgoingPacket(msg);
		}
		
	}

}
