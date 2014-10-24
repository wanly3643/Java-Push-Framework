package org.push.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/**
 * A <code>BroadcastStreamer</code> to manage the packets which are
 * broadcasted to the <code>LogicalConnectionImpl</code>.
 * When calling {@link #awakeAll()}, there will be a command sent to
 * the streamer and it will send the broadcast packets to the client.
 * 
 * ${@link #addItem(LogicalConnectionImpl)} will add the 
 * <code>LogicalConnectionImpl</code> into the processing list, and
 * ${@link #removeItem(LogicalConnectionImpl)} will remove the
 * <code>LogicalConnectionImpl</code> into the processing list.
 * 
 * Note: there will be delay between the method is called and the
 * corresponding action is done because calling the method only send
 * the command and return at once, will not wait until the action is done.
 * 
 * @author Lei Wang
 */

public class BroadcastStreamer {

	private static byte ADD_ITEM = 0x01;
	private static byte REMOVE_ITEM = 0x02;
	private static byte AWAKE_ALL = 0x04;

	private Set<LogicalConnectionImpl> checkList = 
		new HashSet<LogicalConnectionImpl>();

	private Set<LogicalConnectionImpl> newItems = 
		new HashSet<LogicalConnectionImpl>();

	private Set<LogicalConnectionImpl> removedItems = 
		new HashSet<LogicalConnectionImpl>();
	//
	private byte eventBits;
	private int itemsCount;
	private volatile boolean shouldStop;
	private Thread thread;
	private Object csSharedLists;
	
	public BroadcastStreamer() {
		shouldStop = false;
		itemsCount = 0;
		eventBits = 0x00;
		csSharedLists = new Object();
	}
	
	private void processAddAndRemove() {
		if ((eventBits & REMOVE_ITEM) > 0) {
			eventBits &= (~REMOVE_ITEM);
			clearRemovedItems();
		}

		if ((eventBits & ADD_ITEM) > 0) {
			eventBits &= (~ADD_ITEM);
			checkList.addAll(newItems);
			newItems.clear();
		}
	}
	
	private void doWork() {
		while (!shouldStop) {
			if (eventBits == 0) {
				LockSupport.parkNanos(10000000L); // 10 milliseconds
				continue;
			}

			boolean requireLock = (eventBits & (ADD_ITEM | REMOVE_ITEM)) != 0;
			if (requireLock) {
				synchronized(csSharedLists) {
					processAddAndRemove();
				}
			} else {
				processAddAndRemove();
			}

			if ((eventBits & AWAKE_ALL) > 0) {
				eventBits &= (~AWAKE_ALL);
				processAwakedItems(checkList);
				/*checkList.clear();*/
			}
		}
	}

	private void clearRemovedItems() {
		Iterator<LogicalConnectionImpl> it = removedItems.iterator();
		LogicalConnectionImpl removedItem = null;
		boolean found = false;
		while (it.hasNext()) {
			removedItem = it.next();
			if (newItems.remove(removedItem)) {
				found = true;
			}

			if (checkList.remove(removedItem)) {
				found = true;
			}

			if (found) {
				removedItem.decrementUsage();
			}
		}

		removedItems.clear();
	}

	private void processAwakedItems(Set<LogicalConnectionImpl> activeSet) {
		Iterator<LogicalConnectionImpl> it = activeSet.iterator();
		while (it.hasNext()) {
			processItem(it.next());
		}
	}

	private boolean processItem(LogicalConnectionImpl logicalConnection) {
		return logicalConnection.checkAndProcessPendingBroadcast(true);
	}

	public void addItem(LogicalConnectionImpl item) {
		System.out.println("Adding connection into streamer");
		item.incrementUsage();
		synchronized(csSharedLists) {
			newItems.add(item);
			eventBits |= ADD_ITEM;
		}
	}

	public void removeItem(LogicalConnectionImpl item) {
		synchronized(csSharedLists) {
			removedItems.add(item);
			eventBits |= REMOVE_ITEM;
		}
	}

	public void awakeAll() {
		eventBits |= AWAKE_ALL;
	}
	
	public int getItemsCount() { return itemsCount; }

	public void start() {
		final BroadcastStreamer me = this;
		thread = new Thread(new Runnable() {

			public void run() {
				me.doWork();
			}
		});
		
		thread.start();
	}

	public void stop() {
		shouldStop = true;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// release the thread
		thread = null;
	}
}
