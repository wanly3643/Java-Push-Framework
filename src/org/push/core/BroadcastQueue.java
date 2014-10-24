package org.push.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.push.protocol.OutgoingPacket;
import org.push.util.Utils;

/**
 * A <code>BroadcastQueue</code> will be used to maintain the
 * packets broadcasted to multiple connections. The key of the
 * connection and will be stored.
 * A queue of <code>PacketInfo</code> will be maintained
 * as a history.
 * 
 * @author Lei Wang
 */

public class BroadcastQueue {

	private static final Integer DEFAULT_GEN_ID = new Integer(0);
    private BroadcastManagerBase manager;

    private String channelName;
    private QueueOptions queueOptions;

    private Queue<PacketInfo> activeList = new LinkedList<PacketInfo>();

    // Packet which is removed but still being used will be stored here.
    private Queue<PacketInfo> removedList = new LinkedList<PacketInfo>();

    private Map<String, Integer> connectionMap = 
    	new HashMap<String, Integer>();

    private Object cs; // Critical section

    private int cumulatedCount;
    private long lastCountTime;
    private int lastGenId;

    public BroadcastQueue(BroadcastManagerBase manager, String channelName, 
    		QueueOptions queueOptions) {
        this.manager = manager;
        this.channelName = channelName;
        this.queueOptions = queueOptions;

        cs = new Object();
        cumulatedCount = 0;
        lastGenId = 0;
    }

    /**
     * Remove a <code>PacketInfo</code> from the queue
     * If the packet is still being used, it will be 
     * moved to the removedList, otherwise, it will be
     * removed directly
     * 
     * @param packetInfo  the one to be removed
     */
    private void internalRemove(PacketInfo packetInfo) {
        if(packetInfo.hasNoRef()) {
            manager.deleteOutgoingPacket(packetInfo.getPacket());
            
            // For GC
            packetInfo.setPacket(null);
            packetInfo.setNext(null);
            packetInfo.setKillKey(null);
        } else {
            removedList.add(packetInfo);
            packetInfo.setWaitingForRemoval(true);
        }
    }
    
    private boolean checkAgainstMaxFillRate() {
        if (queueOptions.getFillRateThrottlingPeriod() != 0) {
            long timeNow = System.currentTimeMillis();

            if (cumulatedCount == 0) {
                lastCountTime = timeNow;
                cumulatedCount = 1;
            } else {
                if (Utils.timePassed(lastCountTime) < 
                		queueOptions.getFillRateThrottlingPeriod()) {
                    if (cumulatedCount == 
                    	queueOptions.getFillRateThrottlingMaxPackets()) {
                        return false;
                    } else {
                        cumulatedCount += 1;
                    }
                } else {
                    cumulatedCount = 1;
                    lastCountTime = timeNow;
                }
            }
        }
        return true;
    }

    public boolean pushPacket(OutgoingPacket packet) {
    	return pushPacket(packet, "", 0);
    }

    public boolean pushPacket(OutgoingPacket packet, String killKey) {
    	return pushPacket(packet, killKey, 0);
    }

    /**
     * Push a packet into the Queue. A PacketInfo will be
     * added
     * 
     * @param packet
     * @param killKey
     * @param objectCategory
     */
    public boolean pushPacket(OutgoingPacket packet, String killKey, 
    		int objectCategory) {
        synchronized(cs) {
        	if (!checkAgainstMaxFillRate()) {
        		return false;
        	}
        	
        	lastGenId = lastGenId + 1;
            PacketInfo packetInfo = new PacketInfo(packet, killKey, 
            		objectCategory, lastGenId);

            activeList.add(packetInfo);
            if (activeList.size() > queueOptions.getMaxPackets()) {
        		internalRemove(activeList.poll());
        	}
        	return true;
        }
    }

    public void removePacket() {
    	removePacket("", 0);
    }

    public void removePacket(String killKey) {
    	removePacket(killKey, 0);
    }

    public void removePacket(String killKey, int objectCategory) {
    	Utils.nullArgCheck(killKey, "killKey");
        synchronized(cs) {
            PacketInfo packetInfo = null;
            Iterator<PacketInfo> it = activeList.iterator();
            while (it.hasNext()) {
                packetInfo = it.next();
                if (killKey.equals(packetInfo.getKillKey()) && 
                		packetInfo.getObjectCategory() != objectCategory) {
                    internalRemove(packetInfo);
                    it.remove();
                    return;
                }
            }
        }
    }

    /**
     * Subscribe a Connection into this channel, later 
     * the packets sent by this connection will be
     * recorded
     * 
     * @param connectionKey  the key to identify a connection
     */
    public void subscribeClient(String clientKey) {
    	subscribeClient(clientKey, false);
    }

    public void subscribeClient(String clientKey, 
    		boolean ignorePreviousPackets) {
        synchronized(cs) {
            connectionMap.put(clientKey, ignorePreviousPackets ? 
            		new Integer(lastGenId) : DEFAULT_GEN_ID);
        }
    }


    /**
     * Unsubscribe a Connection into this channel, later 
     * the packets sent by this connection will no longer
     * be recorded.
     * 
     * @param connectionKey  the key to identify a connection
     */
    public void unsubscribeClient(String clientKey) {
        synchronized(cs) {
            connectionMap.remove(clientKey);
        }
    }

    /**
     * Decrement a reference of the packet if a packet can
     * be found, and if the packet is no longer needed, it
     * will be removed
     * 
     * @param packetInfo
     * @param connectionKey
     * @param blnSuccess
     */
    public void disposePacket(PacketInfo packetInfo) {
        synchronized(cs) {
        	//In all cases decrement refCount :
            packetInfo.decrementRefCount();
            
            //Check if item is in internal garbage list
            //If found, remove it from the removedList
            if (packetInfo.isWaitingForRemoval() && packetInfo.hasNoRef()) {
            	Iterator<PacketInfo> it = removedList.iterator();
            	while (it.hasNext()) {
            		if (it.next() == packetInfo) {
            			it.remove();
            		}
            	}
            }
        }
    }

    public void disposeAllPackets() {
    	//Delete all packets in the queue.
    	for (PacketInfo packetInfo : activeList) {
    		manager.deleteOutgoingPacket(packetInfo.getPacket());
            
            // For GC
            packetInfo.setPacket(null);
            packetInfo.setNext(null);
            packetInfo.setKillKey(null);
    	}
    	
    	activeList.clear();
    }

    /**
     * Get the packet of the connection by the connectionKey
     * 
     * @param connectionKey
     * @param pPacketInfo
     * @return
     */
    public PacketInfo getNextPacket(int previouslyInsertedId) {
    	synchronized(cs) {
    		// Try to find the PacketInfo by the position value
    		// stored in the map
    		Iterator<PacketInfo> it = activeList.iterator();
    		PacketInfo searchItem = null;
    		boolean found = false;
            while (it.hasNext()) {
                searchItem = it.next();
                if (searchItem.getPacketId() > previouslyInsertedId) {
        			if (queueOptions.getMaxExpireDurationSecs() == 0 || 
        					searchItem.getLife() <= 
        						queueOptions.getMaxExpireDurationSecs()) {
        				found = true;
        				break;
        			}
        		}
            }

            // If found, increment the reference and return the packet
            if (found) {
                searchItem.incrementRefCount();
                return searchItem;
            }
            
            return null;
        }
    }

    public String getChannelName() { return channelName; }

    public int getLastGeneratedId() { return lastGenId; }

    public QueueOptions getOptions() { return queueOptions; }
}
