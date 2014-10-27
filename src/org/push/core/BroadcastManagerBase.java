package org.push.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.push.protocol.OutgoingPacket;

/**
 * This class provide a skeletal implementation of manager to broadcast
 * message to clients
 * 
 * @author Lei Wang
 */

public abstract class BroadcastManagerBase {

	/* To store the broadcast queue by name */
    private Map<String, BroadcastQueue> channelMap = 
    	new HashMap<String, BroadcastQueue>();

    /* To store the names of the broadcast queue */
    private List<String> globalQueues = new LinkedList<String>();;
    
    public BroadcastManagerBase() {
    }

    /**
     * Create a new <code>BroadcastQueue</code> by the provided information
     * @param channelKey   Name of broadcast queue
     * @param queueOptions Options for creating broadcast queue
     */
    public void createBroadcastQueue(String channelKey, 
    		QueueOptions queueOptions) {
        BroadcastQueue queue = new BroadcastQueue(this, channelKey, 
        		queueOptions);

        channelMap.put(channelKey, queue);
    	if (!queueOptions.requireRegistration()) {
    		globalQueues.add(channelKey);
    	}
    }

    /**
     * Remove a broadcast queue by the given name
     * @param channelKey  Name of the broadcast queue
     */
    public void removeBroadcastQueue(String channelKey) {
    	channelMap.remove(channelKey);
    }

    /**
     * @see this{@link #pushPacket(OutgoingPacket, String, String, int)}
     * @param packet
     * @param channelName
     */
    public void pushPacket(OutgoingPacket packet, String channelName) {
    	pushPacket(packet, channelName, "", 0);
    }


    /**
     * @see this{@link #pushPacket(OutgoingPacket, String, String, int)}
     * @param packet
     * @param channelName
     * @param killKey
     */
    public void pushPacket(OutgoingPacket packet, String channelName, 
    		String killKey) {
    	pushPacket(packet, channelName, killKey, 0);
    }

    /**
     * @see push packet into a channel
     * @see {@link BroadcastQueue#pushPacket(OutgoingPacket, String, int)}
     * 
     * @param packet
     * @param channelName
     * @param killKey
     */
    public boolean pushPacket(OutgoingPacket packet, String channelName, 
    		String killKey, int objectCategory) {
    	BroadcastQueue packetQueue = channelMap.get(channelName);
    	if (packetQueue != null) {
    		preEncodeOutgoingPacket(packet);
    		handleOnBeforePushPacket(channelName);
    		
    		//
    		if (packetQueue.pushPacket(packet, killKey, objectCategory)) {
        		activateSubscribers(channelName);
    			return true;
    		}
    	}
 
		deleteOutgoingPacket(packet);
		return false;
    }

    /**
     * Remove packet from a channel
     * @see {@link BroadcastQueue#removePacket(String, int)}
     * 
     * @param killKey
     * @param objectCategory
     * @param channelKey
     */
    public void removePacket(String killKey, int objectCategory, 
    		String channelKey) {
    	BroadcastQueue packetQueue = channelMap.get(channelKey);
    	if (packetQueue != null) {
    		packetQueue.removePacket(killKey, objectCategory);
    	}
    }

    /**
     * dispose all packets of all channels
     * @see BroadcastQueue#disposePacket()
     */
    public void disposeAllPackets() {
    	for (BroadcastQueue packetQueue : channelMap.values()) {
    		packetQueue.disposeAllPackets();
    	}
    }

    /**
     * Return XML format of text which contains the names of queue
     * 
     * @return
     */
    public String getQueuesNames() {
    	StringBuilder sb = new StringBuilder();
    	for (String key : channelMap.keySet()) {
    		sb.append("<queue val=\"").append(key).append("\"/>");
    	}
    	
    	return sb.toString();
    }

    /**
     * Get a <code>BroadcastQueue</code> by the given name
     * @param channelKey  The name of broadcast queue
     * @return  The <code>BroadcastQueue</code>
     */
    public BroadcastQueue getQueue(String channelKey) {
		return channelMap.get(channelKey);
	}

	public List<String> getGlobalQueues() { return this.globalQueues; }
    
    protected abstract void preEncodeOutgoingPacket(OutgoingPacket packet);

    protected abstract void deleteOutgoingPacket(OutgoingPacket packet);
    
    protected abstract void activateSubscribers(String channelName);

    protected void handleOnBeforePushPacket(String channelName) {}
    
    protected void handleOnAfterPacketIsSent(String channelName, 
    		String subscriberKey) {}
}

