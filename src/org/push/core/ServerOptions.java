package org.push.core;

import org.push.util.Utils;

/**
 * Options of the server.
 * 
 * @author Lei Wang
 */

public class ServerOptions {
	
	/**
	 * Max connection allowed by this server
	 */
    private int nMaxConnections;
    
    /**
     * The timeout (seconds) of expiration
     */
    private int uLoginExpireDuration;
    
    
    private boolean challengeClients;
    
    /**
     * Count of worker thread to handle request
     */
    private int nWorkersCount;
    
    /**
     * Count of <code>BroadcatStreamer</code> of this server
     */
    private int nStreamers;
    
    /**
     * Indicate if the state information is stored
     */
    private boolean isProfilingEnabled;
    
    /**
     * The sample rate of the monitor service
     */
    private int samplingRate;
    
    /**
     * Indicate if the monitor service is opened
     */
    private boolean isMonitorEnabled;
    
    /**
     * The service port of the monitor
     */
    private short monitorPort;
    
    /**
     * Password for the monitor service
     */
    private String password;
    
    /**
     * The maximum time allowed while the client is idle
     */
    private int uMaxClientIdleTime;
    
    /**
     * Size of buffer used by socket
     */
    private int nSocketBufferSize;
    
    /**
     * Maximum messages sent to client allowed to pend
     */
    private int nMaxPendingOutgoingMessages;
    
    
    /**
     * 
     */
    private int socketType = 0;

	public ServerOptions() {
		nMaxConnections = 100;
		challengeClients = false;
		uLoginExpireDuration = 35;
		nWorkersCount = Runtime.getRuntime().availableProcessors() * 2;
		nStreamers = 1;
		isProfilingEnabled = false;
		isMonitorEnabled = false;
		monitorPort = 2011;
		samplingRate = 10;
		password = "";
		uMaxClientIdleTime = 120;

		nSocketBufferSize = 8192;
		nMaxPendingOutgoingMessages = 5;
	}
	
	public int getMaxConnections() { return this.nMaxConnections; }
	
	public int getLoginExpireDuration() { return this.uLoginExpireDuration; }
	
	public int getWorkersCount() { return this.nWorkersCount; }
	
	public int getStreamers() { return this.nStreamers; }
	
	public int getSamplingRate() { return this.samplingRate; }
	
	public int getMaxClientIdleTime() { return this.uMaxClientIdleTime; }
	
	public int getSocketBufferSize() { return this.nSocketBufferSize; }
	
	public int getMaxPendingOutgoingMessages() {
		return this.nMaxPendingOutgoingMessages;
	}
	
	public short getMonitorPort() { return this.monitorPort; }
	
	public boolean challengeClients() { return this.challengeClients; }
	
	public boolean isProfilingEnabled() { return this.isProfilingEnabled; }
	
	public boolean isMonitorEnabled() { return this.isMonitorEnabled; }
	
	public String getPassword() { return this.password; }
	
	public int getSocketType() { return socketType; }
	
	public void setMaxConnections(int nMaxConnections) {
		Utils.unsignedIntArgCheck(nMaxConnections, "nMaxConnections");
		
		this.nMaxConnections = nMaxConnections;
	}
	
	public void setLoginExpireDuration(int uLoginExpireDuration) {
		Utils.unsignedIntArgCheck(uLoginExpireDuration, "uLoginExpireDuration");
		
		this.uLoginExpireDuration = uLoginExpireDuration;
	}
	
	public void setWorkersCount(int nWorkersCount) {
		Utils.unsignedIntArgCheck(nWorkersCount, "nWorkersCount");
		
		this.nWorkersCount = nWorkersCount;
	}
	
	public void setStreamers(int nStreamers) {
		Utils.unsignedIntArgCheck(nStreamers, "nStreamers");
		
		this.nStreamers = nStreamers;
	}
	
	public void setSamplingRate(int samplingRate) {
		Utils.unsignedIntArgCheck(samplingRate, "samplingRate");
		
		this.samplingRate = samplingRate;
	}
	
	public void setMaxClientIdleTime(int uMaxClientIdleTime) {
		Utils.unsignedIntArgCheck(uMaxClientIdleTime, "uMaxClientIdleTime");
		
		this.uMaxClientIdleTime = uMaxClientIdleTime;
	}
	
	public void setSocketBufferSize(int nSocketBufferSize) {
		Utils.unsignedIntArgCheck(nSocketBufferSize, "nSocketBufferSize");
		
		this.nSocketBufferSize = nSocketBufferSize;
	}
	
	public void setMaxPendingOutgoingMessage(int nMaxPendingOutgoingMessages) {
		Utils.unsignedIntArgCheck(nMaxPendingOutgoingMessages, 
				"nMaxPendingOutgoingMessages");
		
		this.nMaxPendingOutgoingMessages = nMaxPendingOutgoingMessages;
	}
	
	public void setMonitorPort(short monitorPort) {
		this.monitorPort = monitorPort;
	}
	
	public void setChallengeClients(boolean challengeClients) {
		this.challengeClients = challengeClients;
	}
	
	public void setProfilingEnabled(boolean isProfilingEnabled) {
		this.isProfilingEnabled = isProfilingEnabled;
	}
	
	public void setMonitorEnabled(boolean isMonitorEnabled) {
		this.isMonitorEnabled = isMonitorEnabled;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setSocketType(int socketType) {
		this.socketType = socketType;
	}
}
