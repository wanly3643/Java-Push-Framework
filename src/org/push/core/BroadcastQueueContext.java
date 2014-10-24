package org.push.core;

import org.push.util.Utils;

/**
 * This class is to store the information of <code>BroadcastQueue</code>
 * 
 * @author Lei Wang
 */

public class BroadcastQueueContext {

	private BroadcastQueue queue;
	private int nPacketsSent;
	private int nLastSentPacketId;

	private BroadcastQueueContext next;

	public BroadcastQueueContext(BroadcastQueue queue, int nLastSentPacketId) {
		Utils.unsignedIntArgCheck(nLastSentPacketId, "nLastSentPacketId");
		
		this.nLastSentPacketId = nLastSentPacketId;
		this.nPacketsSent = 0;
		this.next = null;
		this.queue = queue;
	}
	
	public BroadcastQueue getQueue() { return queue; }
	
	public int getPacketsSent() { return nPacketsSent; }
	
	public int getLastSentPacketId() { return nLastSentPacketId; }
	
	public void setQueue(BroadcastQueue queue) {
		this.queue = queue;
	}
	
	public void setPacketsSent(int nPacketsSent) {
		Utils.unsignedIntArgCheck(nPacketsSent, "nPacketsSent");
		this.nPacketsSent = nPacketsSent;
	}
	
	public void setLastSentPacketId(int nLastSentPacketId) {
		Utils.unsignedIntArgCheck(nLastSentPacketId, "nLastSentPacketId");
		this.nLastSentPacketId = nLastSentPacketId;
	}
	
	public BroadcastQueueContext next() { return next; }
	
	public void setNext(BroadcastQueueContext next) {
		this.next = next;
	}

	public boolean hasHigherPriority(BroadcastQueueContext info) {
		QueueOptions myOptions = this.queue.getOptions();
		QueueOptions otherOptions = info.queue.getOptions();

		int myPriority = myOptions.getPriority();
		int otherPriority = otherOptions.getPriority();
		if (myPriority > otherPriority) {
			return true;
		}

		if (myPriority == otherPriority) {
			return myOptions.getPacketsQuota() > otherOptions.getPacketsQuota();
		}

		return false;
	}

	public PacketInfo getNextPacket() {
		return queue.getNextPacket(nLastSentPacketId);
	}

	public void returnPacket(PacketInfo pPacketInfo) {
		nLastSentPacketId = pPacketInfo.getPacketId();
		nPacketsSent += 1;
		queue.disposePacket(pPacketInfo);
	}

	public QueueOptions getQueueOptions() {
		return queue.getOptions();
	}

}
