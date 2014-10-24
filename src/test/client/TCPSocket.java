package test.client;

import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.Protocol;
import org.push.util.CppEnum;
import org.push.util.Utils;

public abstract class TCPSocket {

	public static enum Status implements CppEnum {
        Disconnected(1),
        Connecting(2),
        Connected(3),
        WaitingToClose(4);

        private int value;

        private Status(int value) { this.value = value; }

        public int value() { return this.value; }
	};

	public static enum Result implements CppEnum {
		ResultOK(0),
		ResultTryAgain(1),
		ResultFailed(2);

        private int value;

        private Result(int value) { this.value = value; }

        public int value() { return this.value; }
	};
	
	private TCPSocketImpl pImpl;
	private boolean relayTCPEvents;
	
    public TCPSocket() {
    	this(false);
    }

    public TCPSocket(boolean relayToUserThread) {
    	this.relayTCPEvents = relayToUserThread;
        pImpl = new TCPSocketImpl(this);
    }

    public boolean connect(String hostAddress, int uPort) {
    	Utils.unsignedIntArgCheck(uPort, "uPort");
    	return pImpl.connect(hostAddress, uPort);
    }

    public void registerHandler(int requestId, 
    		ResponseHandler pHandler) {
    	Utils.unsignedIntArgCheck(requestId, "requestId");
    	pImpl.registerHandler(requestId, pHandler);
    }

    public boolean initialize(Protocol protocol, 
    		MessageFactory messageFactory) {
    	return pImpl.initialize(protocol, messageFactory);
    }
    public Result sendRequest(OutgoingPacket packet) {
    	return pImpl.sendRequest(packet);
    }

    public Status getStatus() {
    	return pImpl.getStatus();
    }

    public void disconnect() {
    	disconnect(true);
    }

    public void disconnect(boolean waitForSend) {
    	pImpl.disconnect(waitForSend);
    }

    public void OnReadyToSend() {  }
    public String explainLastError() {
    	return pImpl.explainLastError();
    }
    public long getLastSentDataTime() {
    	return pImpl.lastTimeDataIsSent;
    }

	protected abstract void onConnected();

	protected abstract void onConnectionClosed();

	protected void onPerformAutomatedJob() { }

	protected void onReadyToSendRequests() { }
  
	public void ProcessReturnedEvent(TCPSocketEvent event) {
		if (event.type == TCPSocketEvent.Type.ConnectionEstablished) {
	         onConnected();
	    }

	    if (event.type == TCPSocketEvent.Type.ConnectionClosed)
	    {
	        onConnectionClosed();
	    }
	    if (event.type == TCPSocketEvent.Type.NewMessage)
	    {
	        pImpl.dispatchResponse(event.commandId, event.pPacket);
	    }
	}

	public void PostTCPEvent(TCPSocketEvent event) { }

	public boolean isRelayTCPEvents() { return relayTCPEvents; }
}
