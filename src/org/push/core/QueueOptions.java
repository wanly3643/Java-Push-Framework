package org.push.core;

import org.push.util.Utils;

/**
 * Options for <code>BroadcastQueue</code>
 * 
 * @author Lei Wang
 */

public class QueueOptions {

    private boolean requireRegistration;
    private int maxPackets;
    private int priority;
    private int packetsQuota;
    private int fillRateThrottlingPeriod;
    private int fillRateThrottlingMaxPackets;
    private boolean ignorePreviousPackets;
    private int maxExpireDurationSecs;

	public QueueOptions() {
        requireRegistration = true;
        maxPackets = 100;
        priority = 1;
        packetsQuota = 10;
        fillRateThrottlingPeriod = 0;
        fillRateThrottlingMaxPackets = 0;
		ignorePreviousPackets = false;
		maxExpireDurationSecs = 0;
	}
	
	public boolean requireRegistration() {
		return this.requireRegistration;
	}
	
	public int getMaxPackets() { return this.maxPackets; }
	
	public int getPriority() { return this.priority; }
	
	public int getPacketsQuota() { return this.packetsQuota; }
	
	public int getFillRateThrottlingPeriod() {
		return this.fillRateThrottlingPeriod;
	}
	
	public int getFillRateThrottlingMaxPackets() {
		return this.fillRateThrottlingMaxPackets;
	}
	
	public boolean ignorePreviousPackets() {
		return this.ignorePreviousPackets;
	}
	
	public int getMaxExpireDurationSecs() { return this.maxExpireDurationSecs; }
	
	public void setRequireRegistration(boolean requireRegistration) {
		this.requireRegistration = requireRegistration;
	}
	
	public void setMaxPackets(int maxPackets) {
		Utils.unsignedIntArgCheck(maxPackets, "maxPackets");
		
		this.maxPackets = maxPackets;
	}
	
	public void setPriority(int priority) {
		Utils.unsignedIntArgCheck(priority, "priority");
		
		this.priority = priority;
	}
	
	public void setPacketsQuota(int packetsQuota) {
		Utils.unsignedIntArgCheck(packetsQuota, "packetsQuota");
		
		this.packetsQuota = packetsQuota;
	}
	
	public void setFillRateThrottlingPeriod(int fillRateThrottlingPeriod) {
		Utils.unsignedIntArgCheck(fillRateThrottlingPeriod, 
				"fillRateThrottlingPeriod");
		
		this.fillRateThrottlingPeriod = fillRateThrottlingPeriod;
	}
	
	public void setFillRateThrottlingMaxPackets(int fillRateThrottlingMaxPackets) {
		Utils.unsignedIntArgCheck(fillRateThrottlingMaxPackets, 
				"fillRateThrottlingMaxPackets");
		
		this.fillRateThrottlingMaxPackets = fillRateThrottlingMaxPackets;
	}
	
	public void setIgnorePreviousPackets(boolean ignorePreviousPackets) {
		this.ignorePreviousPackets = ignorePreviousPackets;
	}

}
