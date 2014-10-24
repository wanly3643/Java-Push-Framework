/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.push.protocol;

import org.push.protocol.ErrorCodes.DecodeResult;
import org.push.protocol.ErrorCodes.EncodeResult;
import org.push.util.Utils;

/**
 * This class represents the protocol used. It is also 
 * a node of list which means the protocol list be created.
 * 
 * It is also a pool of <code>ProtocolContext</code>
 * 
 * @author Lei Wang
 */

public abstract class Protocol extends AbstractPool<ProtocolContext> {

    private Protocol upperProtocol;
    private Protocol lowerProtocol;

    public Protocol() {
		upperProtocol = null;
		lowerProtocol = null;
    }
    
    /**
     * Add the protocol as the lower layer of current protocol
     * 
     * @param protocol   the protocol to add
     */
    public void addLowerProtocolLayer(Protocol protocol) {
		lowerProtocol = protocol;
		protocol.upperProtocol = this;
    }
    
    public ProtocolContext createNewProtocolContext() {
		return new ProtocolContext();
    }

	public int getRequiredOutputSize(int maxInputSize) {
        Utils.unsignedIntArgCheck(maxInputSize, "maxInputSize");
 
		return maxInputSize;
    }

	public int getRequiredRecyclableBuffers(int nMaxConnections, 
            int nMaxConcurrentCalls) {
		return 0;
    }
        
    public Protocol getLowerProtocol() { return lowerProtocol; }

    public Protocol getUpperProtocol() { return upperProtocol; }

    public Protocol getLowestProtocol() {
        if (lowerProtocol != null) {
            return lowerProtocol.getLowestProtocol();
        }

        return this;
    }

    @Override
    protected void deleteImpl(ProtocolContext context) {
        //
    }

    @Override
    protected ProtocolContext createImpl() {
		return createNewProtocolContext();
    }

    @Override
    protected void recycleObject(ProtocolContext context) {
		context.recycleIntern();
    }

	public abstract void startSession(ProtocolContext context, 
            Buffer outgoingBytes);

	public abstract boolean readData(ProtocolContext context, 
            Buffer incomingBytes);

	public abstract DecodeResult tryDecode(ProtocolContext context, 
            Buffer outputBuffer);

	public abstract EncodeResult encodeContent(ProtocolContext context, 
            Buffer inputBuffer, Buffer outputBuffer);
    
}
