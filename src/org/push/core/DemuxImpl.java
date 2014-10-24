package org.push.core;

import java.util.LinkedList;
import java.util.List;

import org.push.core.IOEvent.IOEventType;
import org.push.util.Debug;
import org.push.util.Utils;

/**
 * This class will manage lots of worker thread. Each worker thread
 * will try to get I/O event from the <code>IOQueue</code> and call
 * the <code>Dispatcher</code> to handle the event.
 * 
 * Usually there are two kinds of event: read/write. In Java, the
 * write event is different from C++, the write event will be always be
 * triggered only when the socket is writable. So In Java version, 
 * the write event will not be monitored in the <code>IOQueue</code>.
 * So threads for handling write event will not work just be blocked
 * there. Just for consistency with the C++ version, keep the thread
 * for write event, these will be removed in the later version.
 * 
 * @author Lei Wang
 */

public class DemuxImpl {

	private Demultiplexor facade;
	
	private List<Thread> workersThreadsVect = new LinkedList<Thread>();
	
	private volatile boolean stopWorking;

	private ServerImpl serverImpl;

	public DemuxImpl(Demultiplexor facade, ServerImpl serverImpl) {
	    this.facade = facade;
	    this.stopWorking = false;

		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	}
	
	public Demultiplexor getFacade() { return facade; }
	
	public boolean start() {
	    stopWorking = false;
		ServerOptions options = serverImpl.getServerOptions();

	    int nThreads = options.getWorkersCount();
	    Thread thread;
	    final DemuxImpl me = this;
	    for (int i = 0; i < nThreads; i ++) {
	    	thread = new Thread(new Runnable() {
				public void run() {
					threadProcForReadEvents(me);
				}
	    	});
	    	thread.start();
	        workersThreadsVect.add(thread);

	    	thread = new Thread(new Runnable() {
				public void run() {
					threadProcForWriteEvents(me);
				}
	    	});
	    	thread.start();
	        workersThreadsVect.add(thread);
	    }
	    return true;
	}

	public void stop() {
	    stopWorking = true;
	    if (workersThreadsVect != null) {
		    for (Thread thread : workersThreadsVect) {
		    	try {
					thread.join();
				} catch (InterruptedException e) {
					// Ignore;
				}
		    }
	    }
	}

	private static void threadProcForReadEvents(DemuxImpl me) {
	    me.proc();
	}

	private static void threadProcForWriteEvents(DemuxImpl me) {
	    me.proc(false);
	}
	
	private void proc() {
		proc(true);
	}
	
	private void proc(boolean processReadEvents) {
		IOQueue<PhysicalConnection> ioQueue = serverImpl.getIOQueue();
		Dispatcher dispatcher = serverImpl.getDispatcher();

	    IOEvent<PhysicalConnection> ioEvent;
	    while (!stopWorking) {
	    	ioEvent = ioQueue.getQueuedEvent(processReadEvents);

	        if (ioEvent != null) {		        
		        PhysicalConnection perSocketContext = ioEvent.context();

	            //
	            if (ioEvent.type() == IOEventType.write) { // OUT Event
	        		Debug.debug("Out Event for: " + 
	        				perSocketContext.getPeerIP());
	                dispatcher.handleWrite(perSocketContext, -1);
	            }
	            else if (ioEvent.type() == IOEventType.read) { // IN event
	        		Debug.debug("In Event for: " + 
	        				perSocketContext.getPeerIP());
	                dispatcher.handleRead(perSocketContext, -1);
	            }
	        }
	    }
	}
}
