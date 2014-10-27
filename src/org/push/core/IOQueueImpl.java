package org.push.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.push.core.IOEvent.IOEventType;

/**
 * A implementation by Java NIO to manage socket I/O event.
 * In Java, the write event is something different, and it
 * will be triggered all the times when the socket buffer
 * is available so it will occupy the CPU all the times, so
 * write event will not monitored.
 * 
 * @author Lei Wang
 */

public class IOQueueImpl implements IOQueue<PhysicalConnection> {

	private Selector inPollSelector;
	
	private Thread inPollThread;
	
	private volatile boolean isPolling;
	
	private BlockingQueue<SelectionKey> inPollQueue;
	
	private ConcurrentMap<PushClientSocket, SelectionKeyGroup> socketMap
		= new ConcurrentHashMap<PushClientSocket, SelectionKeyGroup>();
	
	private static class SelectionKeyGroup {
		private SelectionKey readKey;
		private SelectionKey writeKey;
		
		SelectionKeyGroup(SelectionKey readKey, SelectionKey writeKey) {
			this.readKey = readKey;
			this.writeKey = writeKey;
		}
		
		SelectionKey getReadKey() { return this.readKey; }
		SelectionKey getWriteKey() { return this.writeKey; }
	}
	
	private static void closeSelector(Selector selector) {
		if (selector == null) {
			return;
		}

		try {
			selector.close();
		} catch (IOException e1) {
			// Ignore
		}
	}

	public boolean create() {
		int pollsize = 1024;
		
		try {
			inPollSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		inPollQueue = new LinkedBlockingQueue<SelectionKey>(pollsize);
		
		isPolling = true;
		final IOQueueImpl me = this;
		inPollThread = new Thread(new Runnable() {
			public void run() {
				me.pollEvents(false);
			}
		});
		inPollThread.start();
		
		return true;
	}

	public void free() {
		isPolling = false;

		closeSelector(inPollSelector);
		inPollSelector = null;
		
		// Clear the event queue first
		// will awake the thread if it blocks because the queue is full.
		inPollQueue.clear();

		try {
			inPollThread.join();
		} catch (InterruptedException e) {
			// Ignore
		}
		
		// Clear registered sockets
		socketMap.clear();
	}

	public IOEvent<PhysicalConnection> getQueuedEvent(boolean isInputEvents) {
		final IOEventType type;
		final BlockingQueue<SelectionKey> pollQueue;
		
		if (isInputEvents) {
			type = IOEventType.read;
			pollQueue = inPollQueue;
		} else {
			type = null;
			pollQueue = null;
		}
		
		if (pollQueue == null) {
			return null;
		}

		try {
			SelectionKey key = pollQueue.poll(1000L, TimeUnit.MILLISECONDS);
			if (key != null) {
				if (key.attachment() instanceof PhysicalConnection) {
					return new IOEvent<PhysicalConnection>(type, 
							(PhysicalConnection)(key.attachment()));
				}
			}
		} catch (InterruptedException e) {
			// Ignore
		}
		
		return null;
	}

	public boolean addSocketContext(PushClientSocket socket,
			PhysicalConnection context) {		
		// Register the events to listen
		SelectionKey readKey = null;
		SelectionKey writeKey = null;
		try {
			readKey = socket.registerSelector(inPollSelector, 
					SelectionKey.OP_READ, context);
			System.out.println("Register socket for read");
//			writeKey = socket.registerSelector(outPollSelector, 
//					SelectionKey.OP_WRITE, context);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		socketMap.put(socket, new SelectionKeyGroup(readKey, writeKey));

		return true;
	}

	public void deleteSocketContext(PushClientSocket socket) {
		SelectionKeyGroup keyGroup = socketMap.remove(socket);
		if (keyGroup != null) {
			SelectionKey readKey = keyGroup.getReadKey();
			SelectionKey writeKey = keyGroup.getWriteKey();
			if (readKey != null) {
				// Cancel monitoring read event 
				readKey.cancel();
			}
			
			if (writeKey != null) {
				// Cancel monitoring write event 
				writeKey.cancel();
			}
		}
	}
	
	private void pollEvents(boolean isOutPoll) {
		Selector selector;
		BlockingQueue<SelectionKey> queue;
		if (isOutPoll) {
			return;
		} else {
			selector = this.inPollSelector;
			queue = this.inPollQueue;
		}
		
		List<SelectionKey> cache = new LinkedList<SelectionKey>();

		while (isPolling) {
			try {
				selector.select();
			} catch (IOException e) {
				continue;
			}
			
			// yield for register
			Thread.yield();
			
			// Add into cache (Add into the blocking queue directly
			// may block so that the selector cannot release the selection 
			// key in time)
			if (selector.isOpen()) {
				for (SelectionKey key : selector.selectedKeys()) {
					key.cancel(); // cancel the key until it is processed
					cache.add(key);
				}
				
				// Clear the keys
				selector.selectedKeys().clear();
				
				// Move from cache to queue (May block if the queue is full)
				// The free() method will call clear() first to awake the 
				// thread first and join the thread
				queue.addAll(cache);
				cache.clear();
			} else {
				break; // selector closed
			}
		}
	}

	public boolean rearmSocketForWrite(PushClientSocket socket,
			PhysicalConnection context) {
		return false;
	}

	public boolean rearmSocketForRead(PushClientSocket socket,
			PhysicalConnection context) {
		SelectionKey readKey = null;
		try {
			readKey = socket.registerSelector(inPollSelector, 
					SelectionKey.OP_READ, context);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		SelectionKeyGroup keyGroup = socketMap.get(socket);
		if (keyGroup != null) {
			keyGroup.readKey = readKey;
			return true;
		}

		return false;
	}
}
