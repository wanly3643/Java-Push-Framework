package test.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.push.protocol.Buffer;
import org.push.protocol.Connection;
import org.push.protocol.DeserializeData;
import org.push.protocol.ErrorCodes.NetworkDeserializeResult;
import org.push.protocol.ErrorCodes.NetworkSerializeResult;
import org.push.protocol.BufferPool;
import org.push.protocol.IncomingPacket;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolManager;
import org.push.protocol.RecyclableBuffer;
import org.push.protocol.SerializeData;
import org.push.util.Debug;
import org.push.util.Utils;

public class TCPSocketImpl extends Connection {
	
	private static final int nSocketBufferSize = 8192;
	
	private TCPSocket pFacade;

	private TCPSocket.Status status;

	private String strIP;
	private int uPort;

	private SocketChannel hSocket;
	private Selector selector;

	private Thread hThread;

	private Buffer oBuffer = new Buffer();

	private Protocol pProtocol;
	private MessageFactory messageFactory;

	private Map<Integer, ResponseHandler> handlerMap
		= new HashMap<Integer, ResponseHandler>();

	private Object cs;

	//private ProtocolContext pContext;

	private ProtocolManager theProtocolManager = new ProtocolManager();
	
	public long lastTimeDataIsSent;

	//private boolean bSendInProgress;

	private volatile boolean stopClient;

	private void doNetworkJobs() {
		List<SelectionKey> cache = new LinkedList<SelectionKey>();
		boolean needBreak = false;
		while (!needBreak) {
			try {
				selector.select(10000);
			} catch (IOException e) {
				// 
				OnClose();
				break;
			}

			if (stopClient) {
				break;
			}

			SelectionKey key;
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				key = it.next();
				
				it.remove();
		        //Dispatch event :
		        if (key.isReadable()) {
		        	Debug.debug("read Event");
		        	if(!OnRead()) {
		        		needBreak = true;
		                break;
		        	}
		        }
		        
		        if (key.isWritable()) {
		        	Debug.debug("write Event");
		            if(!OnWrite()) {
		        		needBreak = true;
		                break;
		        	}
		        }
			}
			
			cache.clear();
	    }

	    status = TCPSocket.Status.Disconnected;
	    
	    disconnect(false);

		if (!stopClient)
		{
			if (pFacade.isRelayTCPEvents())
			{
				TCPSocketEvent pEvent = new TCPSocketEvent();
				pEvent.type = TCPSocketEvent.Type.ConnectionClosed;
				pFacade.PostTCPEvent(pEvent);
			}
			else {
				pFacade.onConnectionClosed();
			}
		}
	    
	}

	private boolean OnRead() {
		RecyclableBuffer incomingBytes = new RecyclableBuffer();
		if (!ReadReceivedBytes(incomingBytes)) {
			incomingBytes.release();
			return false;
		}

		if (!theProtocolManager.processIncomingData(this, incomingBytes)) {
			Debug.debug("ProtocolManager failed to process");
			incomingBytes.release();
			return false;
		}

		DeserializeData deserializeData = new DeserializeData(
				getProtocol().getLowestProtocol());
		NetworkDeserializeResult result;

		while (true) {
			deserializeData.clear();
			result = theProtocolManager.tryDeserializeIncomingPacket(this, 
					deserializeData);
		
			if (result == NetworkDeserializeResult.ProtocolBytes) {	
				if (deserializeData.getProtocolBytes().hasBytes()) {
					sendBufferBytesRequest(deserializeData.getProtocolBytes(), 
							deserializeData.getProtocol().getLowerProtocol());
				}

				continue;
			} else if (result == NetworkDeserializeResult.Content) {
				dispatchResponse(deserializeData.getRoutingService(), 
						deserializeData.getMessage());

				messageFactory.disposeIncomingPacket(
						deserializeData.getMessage());
			} else if(result == NetworkDeserializeResult.WantMoreData) {
				break;
			} else {
			/*result == NetworkDeserializeResult::Failure or 
			NetworkDeserializeResult::Initializationfailure*/
				Debug.debug("Unknown result: " + result);
				break;
			}	
		}
		
		deserializeData.release();
		incomingBytes.release();

		if (result != NetworkDeserializeResult.WantMoreData) {
			return false;
		}

		return true;
	}

	private boolean OnWrite() {
		lastTimeDataIsSent = System.currentTimeMillis();

		//
	    //bSendInProgress = false;

	    if (oBuffer.getDataSize() == 0)
	    {
	        pFacade.OnReadyToSend();
	        return true;
	    }

	    return WriteBytes();
	}
	private boolean OnConnect() {
		synchronized(cs) {
		    status = TCPSocket.Status.Connected;
	
			//Advance initialization.
			RecyclableBuffer protocolBytes = new RecyclableBuffer();
			Protocol pProtocol = advanceInitialization(protocolBytes);
			if (pProtocol != null) {
				sendBufferBytesRequest(protocolBytes, 
						pProtocol.getLowerProtocol());
			}
			
			protocolBytes.release();
	
			checkConnectionInitialization();
	
		    return true;
		}
	}
	private boolean OnClose() { return false; }

	private boolean WriteBytes() {
	    if (oBuffer.getDataSize() == 0) {
	        return true;
	    }

	    int uSize = Math.min(nSocketBufferSize, oBuffer.getDataSize());
	    
	    ByteBuffer buffer = ByteBuffer.allocateDirect(uSize);
	    buffer.put(oBuffer.getBuffer(), 0, oBuffer.getDataSize());

	    buffer.flip();
	    try {
	    	while (buffer.hasRemaining()) {
	    		System.out.println(hSocket.write(buffer) + " bytes sent");
	    	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}


		lastTimeDataIsSent = System.currentTimeMillis();
        oBuffer.pop(uSize);
        
        return WriteBytes();
	}

	String getLastErrorStdStr() {
		return "";
	}

	private boolean ReadReceivedBytes(Buffer incomingBytes) {
		int dwSize = 0;
		
		if (!hSocket.isConnected()) {
			Debug.debug("socket has been closed");
			return false;
		}
		
		ByteBuffer buf = ByteBuffer.allocate(1024);
		
		try {
			dwSize = hSocket.read(buf);
		} catch (IOException e) {
			e.printStackTrace();
			closeSocket(hSocket);
			closeSelector(selector);
			return false;
		}
		
		System.out.println(dwSize + " bytes read from server");

		if(dwSize <= 0)
			return false;
		
		incomingBytes.clearBytes();
		
		incomingBytes.append(buf.array(), dwSize);

		return true;
	}

	public TCPSocketImpl(TCPSocket pFacade) {
		this.pFacade = pFacade;

	    status = TCPSocket.Status.Disconnected;

	    hSocket = null;

	    hThread = null;

	    //bSendInProgress = false;

		messageFactory = null;
		pProtocol = null;

	    cs = new Object();
	}
	
	private static InetAddress getAddressFromName(String name){
		String strHost = name.substring(name.indexOf('@') + 1);
		
		try {
			return InetAddress.getByName(strHost);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public Protocol getProtocol() {
		return pProtocol;
	}

	@Override
	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	@Override
	protected void initializeConnection() {
		pFacade.onConnected();
	}
	
	public boolean initialize(Protocol protocol, 
			MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
		this.pProtocol = protocol;

		//Ensure that receive buffer is as big as twice the socket buffer.
		int nSize = Math.max( calculateMaxBufferSizePerMessage(), 
				nSocketBufferSize);

		if(!BufferPool.getDefaultPool().create(RecyclableBuffer.Type.Single, 
				10, nSize))
			return false;

		if(!BufferPool.getDefaultPool().create(RecyclableBuffer.Type.Double, 
				10, nSize*2))
			return false;

		oBuffer.allocate(nSize * 5);


		pProtocol.initialize(1);

		this.setUpProtocolContexts();

		//
		return true;
	}

    public TCPSocket.Status getStatus() { return status; }

    public boolean connect(String hostAddress, int uPort) {
    	Utils.unsignedIntArgCheck(uPort, "uPort");
    	
    	synchronized(cs) {
	        if(status != TCPSocket.Status.Disconnected) {
	        	Debug.debug("Already connected");
	            return true;
	        }
	    	
	    	if ("255.255.255.255".equals(hostAddress)) {
	    		InetAddress ia = getAddressFromName(hostAddress);
	    		if (ia == null) {
		        	Debug.debug("Unknown host: " + hostAddress);
	    			//strError = getLastErrorStdStr();
	    			return false;
	    		}
	    		this.strIP = ia.getHostAddress();
	    	} else {
	    		this.strIP = hostAddress;
	    	}
	
	        this.uPort = uPort;	        
	
	        try {
				hSocket = SocketChannel.open();
				
		        status = TCPSocket.Status.Connecting;
		        
		        if (!hSocket.connect(new InetSocketAddress(strIP, 
		        		this.uPort))) {
		        	Debug.debug("Failled to connect " + strIP + ":" + uPort);
		        	closeSocket(hSocket);

					closeSelector(selector);
					selector = null;
		        	return false;
		        }
				selector = Selector.open();
				hSocket.configureBlocking(false);
		        hSocket.register(selector, SelectionKey.OP_READ);
		        //hSocket.register(selector, SelectionKey.OP_WRITE);
			} catch (IOException e) {
				e.printStackTrace();
				closeSocket(hSocket);
				hSocket = null;

				closeSelector(selector);
				selector = null;
				return false;
			}
			
			this.OnConnect();

	        stopClient = false;
	
	        final TCPSocketImpl me = this;
	        hThread = new Thread(new Runnable() {

				public void run() {
					me.doNetworkJobs();
				}
	        	
	        });
	        
	        hThread.start();
	
	        return true;
    	}
    }
    public void registerHandler(int requestId, ResponseHandler pHandler) {
    	handlerMap.put(new Integer(requestId), pHandler);
    }

    public TCPSocket.Result sendRequest(OutgoingPacket pPacket) {

    	//m_stats.nMessagesOut ++;messageFactory

    	SerializeData serializeData = new SerializeData(getProtocol());
    	NetworkSerializeResult result = 
    		theProtocolManager.serializeOutgoingPacket(this, pPacket, 
    				oBuffer, serializeData);

    	if (result != NetworkSerializeResult.Success)
    	{
    		//stats.addToCumul(ServerStats::BandwidthRejection, uWrittenBytes);
    		return result == NetworkSerializeResult.Retry ? 
    				TCPSocket.Result.ResultTryAgain : 
    					TCPSocket.Result.ResultFailed; //TODO. There is the fatal protocol failure case.
    	}

    	if (WriteBytes())
    	{
    		return TCPSocket.Result.ResultOK;
    	}
    	else
    	{
    		return TCPSocket.Result.ResultFailed;
    	}
    }

	public TCPSocket.Result sendBufferBytesRequest(Buffer protocolBytes, 
			Protocol pProtocol) {
		SerializeData serializeData = new SerializeData(pProtocol);
		NetworkSerializeResult result = 
			theProtocolManager.serializeOutgoinBytes(this, protocolBytes, 
					oBuffer, serializeData);

		if (result != NetworkSerializeResult.Success)
		{
			//stats.addToCumul(ServerStats::BandwidthRejection, uWrittenBytes);
			return result == NetworkSerializeResult.Retry ? 
					TCPSocket.Result.ResultTryAgain : 
						TCPSocket.Result.ResultFailed; //TODO. There is the fatal protocol failure case.
		}

		if (WriteBytes())
		{
			return TCPSocket.Result.ResultOK;
		}
		else
		{
			//m_stats.nMessagesOutFailed++;
			return TCPSocket.Result.ResultFailed;
		}
	}
    public void disconnect(boolean waitForSend) {
    	if (status == TCPSocket.Status.Disconnected) {
    		return; // Nothing to do.
    	}

        if (waitForSend && oBuffer.getDataSize() != 0)
        {
            status = TCPSocket.Status.WaitingToClose;
            return;
        }
        
        stopClient = true;

        closeSocket(hSocket);
        closeSelector(selector);
        //Stop the processing thread.

        try {
			hThread.join();
		} catch (InterruptedException e) {
			// ignore
		}
        hThread = null;

        status = TCPSocket.Status.Disconnected;
    }
    
    private static void closeSocket(SocketChannel hSocket) {
    	if (hSocket == null || !hSocket.isOpen()) { return; }

    	try {
			hSocket.close();
		} catch (IOException e) {
			// ignore;
		}
    }
    
    private static void closeSelector(Selector selector) {
    	if (selector == null || !selector.isOpen()) { return; }

    	try {
    		selector.close();
		} catch (IOException e) {
			// ignore;
		}
    }

    public void dispatchResponse(int requestId, IncomingPacket packet) {
        ResponseHandler pHandler = handlerMap.get(new Integer(requestId));
        if (pHandler != null) {
        	pHandler.handleResponse(packet);
        }
    }

    public String explainLastError() {
    	return "";
    }

	public void LogStatistics() { //m_stats.Log(); 
	}

	public int calculateMaxBufferSizePerMessage() {
		int nSize = messageFactory.getMaximumMessageSize();
		Protocol protocol = getProtocol();
		while (protocol != null) {
			nSize = protocol.getRequiredOutputSize(nSize);
			protocol = protocol.getLowerProtocol();
		}

		return nSize;
	}

}
