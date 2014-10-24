package org.push.core;

import org.push.protocol.Protocol;

/**
 * This class contains all the options for a <code>Listener</code>
 * 
 * @author Lei Wang
 *
 */

public class ListenerOptions {

	private String interfaceAddress;

    private int listeningBackLog;
    private boolean isTcpNoDelay;
    private boolean synAttackPrevention;

    private Protocol protocol;

    public ListenerOptions() {
        interfaceAddress = null;
        listeningBackLog = 100;
        isTcpNoDelay = true;
        synAttackPrevention = false;
        protocol = null;
    }
    
    public String getInterfaceAddress() {
    	return this.interfaceAddress;
    }
    
    public int getListeningBackLog() {
    	return this.listeningBackLog;
    }
    
    public boolean isTcpNoDelay() {
    	return this.isTcpNoDelay;
    }
    
    public boolean synAttackPrevention() {
    	return this.synAttackPrevention;
    }
    
    public Protocol getProtocol() {
    	return this.protocol;
    }
    
    public void setInterfaceAddress(String interfaceAddress) {
    	this.interfaceAddress = interfaceAddress;
    }
    
    public void setListeningBackLog(int listeningBackLog) {
    	this.listeningBackLog = listeningBackLog;
    }
    
    public void setIsTcpNoDelay(boolean isTcpNoDelay) {
    	this.isTcpNoDelay = isTcpNoDelay;
    }
    
    public void setSynAttackPrevention(boolean synAttackPrevention) {
    	this.synAttackPrevention = synAttackPrevention;
    }
    
    public void setProtocol(Protocol protocol) {
    	this.protocol = protocol;
    }
}
