package org.push.core;

import org.push.protocol.OutgoingPacket;
import org.push.util.Utils;


/**
 * A linked list implementation to store a list of
 * information of <code>OutgoingPacket</code>
 * 
 * @author Lei Wang
 *
 */

public class PacketInfo {

	private int packetId;

	private OutgoingPacket packet;

	// The key to identify the PacketInfo when removed
	// objectCategory will be checked.
	private String killKey;

	// 
	private int objectCategory;

	private int refCount;
	
	// Mean it is in the removedList
	private boolean blnWaitingForRemoval;
	
	private long creationTime;

	private PacketInfo next;
    
    public PacketInfo(OutgoingPacket packet, String killKey, int objectCategory, int packetId) {
    	this.packetId = packetId;
        this.next = null;
        this.packet = packet;
        this.killKey = killKey;
        this.objectCategory = objectCategory;
        this.refCount = 0;
        this.blnWaitingForRemoval = false;
        this.creationTime = System.currentTimeMillis();
    }
    
    public void incrementRefCount() {
    	this.refCount = this.refCount + 1;
    }
    
    public void decrementRefCount() {
    	this.refCount = this.refCount - 1;
    }
    
    public void setRefCount(int refCount) {
    	this.refCount = refCount;
    }
    
    public int getRefCount() {
    	return this.refCount;
    }
    
    public boolean hasNoRef() {
    	return this.refCount == 0;
    }
    
    public double getLife() {
		return Utils.timePassed(creationTime);
	}
    
    public int getPacketId() { return packetId; }
    
    public void setPacketId(int packetId) { this.packetId = packetId; }
    
    public OutgoingPacket getPacket() { return packet; }
    
    public void setPacket(OutgoingPacket packet) { this.packet = packet; }
    
    public String getKillKey() { return killKey; }
    
    public void setKillKey(String killKey) { this.killKey = killKey; }

    public int getObjectCategory() { return objectCategory; }
    
    public void setObjectCategory(int objectCategory) {
    	this.objectCategory = objectCategory;
    }

	public boolean isWaitingForRemoval() { return blnWaitingForRemoval; }

	public void setWaitingForRemoval(boolean blnWaitingForRemoval) {
		this.blnWaitingForRemoval = blnWaitingForRemoval;
	}
	
	public PacketInfo next() { return next; };
	
	public void setNext(PacketInfo next) {
		this.next = next;
	}
}