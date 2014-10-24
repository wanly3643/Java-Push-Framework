package org.push.protocol;

import org.push.protocol.ErrorCodes.ContextStates;

/**
 * This class represents the state of the data by some protocol.
 * It will save the data with the format defined by the protocol
 * and the state to indicate if it is initialized. 
 * 
 * @author Lei Wang
 */
public class ProtocolContext {

	private RecyclableBuffer receivedData;
	private short state;
    
	public ProtocolContext() {
        this.receivedData = new RecyclableBuffer(RecyclableBuffer.Type.Double);
        this.state = 0;
    }
	
	/**
	 * Return the binary data of current protocol
	 * 
	 * @return
	 */
	public Buffer getDataBuffer() {
		return receivedData;
    }

	public boolean isInitialized() {
		return (state & ContextStates.InitEnded.value()) != 0;
    }

	public boolean isInitializationStarted() {
		return (state & ContextStates.InitStarted.value()) != 0;
    }

	public void setInitialized() {
		state |= ContextStates.InitEnded.value();
	}

	public void setInitializationStarted() {
        state |= ContextStates.InitStarted.value();
    }
	
	/**
	 * Clear the saved state of the protocol
	 */
	public void recycleIntern() {
		receivedData.clearBytes();
		state = 0;
		recycle();
	}

	protected void recycle() {
		//receivedData.release();
    }
}
