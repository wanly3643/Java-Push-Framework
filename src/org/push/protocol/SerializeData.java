package org.push.protocol;

import org.push.util.Utils;

/**
 * This class will store the state during the serialization of the message.
 * 
 * @author Lei Wang
 */

public class SerializeData {

    private int nEncodingBytes;
    private int nSerializationBytes;
    private Protocol protocol;
    
    public SerializeData(Protocol protocol) {
		this.nEncodingBytes = 0;
		this.nSerializationBytes = 0;
		this.protocol = protocol;
	}
    
    public int getEncodingBytes() { return this.nEncodingBytes; }
    
    public void setEncodingBytes(int nEncodingBytes) {
        Utils.unsignedIntArgCheck(nEncodingBytes, "nEncodingBytes");
        this.nEncodingBytes = nEncodingBytes; 
    }
    
    public int getSerializationBytes() { return this.nSerializationBytes; }
    
    public void setSerializationBytes(int nSerializationBytes) {
        Utils.unsignedIntArgCheck(nSerializationBytes, "nSerializationBytes");
        this.nSerializationBytes = nSerializationBytes; 
    }
    
    public Protocol getProtocol() { return this.protocol; }
    
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    
}
