package org.push.core;

/**
 * To maintain a list of <code>BroadcastQueueContext</code>
 * 
 * @author Lei Wang
 */

public class BroadcastQueueGroupContext {

	private BroadcastQueueContext rootItem;
	private BroadcastQueueContext current;
	private BroadcastQueueGroupContext nextGroup;

	public BroadcastQueueGroupContext(BroadcastQueueContext queueContext) {
		rootItem = queueContext;
		current = queueContext;
		nextGroup = null;
	}
	
	public BroadcastQueueContext root() { return rootItem; }
	
	public BroadcastQueueGroupContext next() { return nextGroup; }
	
	public void setNext(BroadcastQueueGroupContext nextGroup) {
		this.nextGroup = nextGroup;
	}

	public int getPriority() {
		return rootItem.getQueueOptions().getPriority();
	}

	public boolean insert(BroadcastQueueContext info) {

		if(getPriority() < info.getQueueOptions().getPriority()) {
			return false;
		}

		if (info.getQueueOptions().getPriority() < getPriority()) {
			if(nextGroup == null) {
				nextGroup = new BroadcastQueueGroupContext(info);
			} else {
				if (!nextGroup.insert(info)) {
					BroadcastQueueGroupContext newGroupContext = 
						new BroadcastQueueGroupContext(info);
					newGroupContext.nextGroup = nextGroup;
					nextGroup = newGroupContext;
				}
			}
			return true;
		}

		//Same priority.
		if (info.getQueueOptions().getPacketsQuota() > 
				rootItem.getQueueOptions().getPacketsQuota()) {
			info.setNext(rootItem);
			rootItem = info;
		} else {
			BroadcastQueueContext immediateParent = rootItem;
			while (immediateParent.next() != null && 
					immediateParent.next().getQueueOptions().getPacketsQuota() > 
						info.getQueueOptions().getPacketsQuota()) {
				immediateParent = immediateParent.next();
			}
			info.setNext(immediateParent.next());
			immediateParent.setNext(info);
		}
		return true;
	}
	public void remove(String queueName) {
		BroadcastQueueContext pSearch = rootItem;
		BroadcastQueueContext pPrevious = null;
		while (pSearch != null) {
			if (queueName != null && queueName.equals(pSearch.getQueue().getChannelName())) {
				break;
			}

			pPrevious = pSearch;
			pSearch = pSearch.next();
		}


		if (pSearch != null)
		{
			if (pPrevious != null) {
				pPrevious.setNext(pSearch.next());
			} else {
				rootItem.setNext(pSearch.next());
			}

			pSearch.setNext(null);

			if (current == pSearch) {
				current = null;
			}
			
			pSearch = null;
		}
		else {
			if (nextGroup != null) {
				nextGroup.remove(queueName);

				if (nextGroup.rootItem == null) {
					//Next group has become empty. remove it.
					BroadcastQueueGroupContext pTemp = nextGroup;

					this.nextGroup = pTemp.nextGroup;
					pTemp.nextGroup = null;

					pTemp = null;
				}
			}			
		}
	}

	public PacketInfo getNextPacket() {
		BroadcastQueueContext context = current;

		PacketInfo packetInfo;
		while (true) {
			packetInfo = context.getNextPacket();
			if (packetInfo != null) {
				current = context;
				return packetInfo;
			}
			//
			context = context.next() != null ? context.next() : rootItem;

			if (context == current) {
				return null;
			}
		}
	}
	public void returnPacket(PacketInfo pPacketInfo) {
		returnPacket(pPacketInfo, true);
	}

	public void returnPacket(PacketInfo packetInfo, boolean isSent) {
		if (isSent) {
			current.setLastSentPacketId(packetInfo.getPacketId());;
			current.setPacketsSent(current.getPacketsSent() + 1);
		}
		
		current.getQueue().disposePacket(packetInfo);
		
		if (current.getPacketsSent() >= 
				current.getQueueOptions().getPacketsQuota()) {
			current.setPacketsSent(0);
			current = current.next() != null ? current.next() : rootItem;
		}
	}

}
