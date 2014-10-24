package org.push.protocol;

import org.push.util.Releasable;

/**
 * This class will store the state during the de-serialization of 
 * the binary data.
 * 
 * @author Lei Wang
 */

public class DeserializeData implements Releasable {

    private Protocol protocol;
    private IncomingPacket message;
    private int nRoutingService;
    private RecyclableBuffer protocolBytes = new RecyclableBuffer();
    
    public DeserializeData(Protocol protocol) {
		this.protocol = protocol;
		clear();
    }

    public final void clear() {
		protocolBytes.clearBytes();
		message = null;
		nRoutingService = 0;
    }
    
    public Protocol getProtocol() { return this.protocol; }
    
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    
    public int getRoutingService() { return this.nRoutingService; }
    
    public void setRoutingService(int nRoutingService) {
        this.nRoutingService = nRoutingService;
    }
    
    public IncomingPacket getMessage() { return this.message; }
    
    public void setMessage(IncomingPacket message) {
        this.message = message;
    }
    
    public RecyclableBuffer getProtocolBytes() {
        return this.protocolBytes;
    }
    
    public void setProtocolBytes(RecyclableBuffer protocolBytes) {
        this.protocolBytes = protocolBytes;
    }

	public void release() {
		protocolBytes.release();
	}
}
