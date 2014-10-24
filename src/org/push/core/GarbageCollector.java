package org.push.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.push.util.Utils;

/**
 * To manage garbage collection of <code>LogicalConnectionImpl</code>
 * 
 * @author Lei Wang
 */

public class GarbageCollector {

	private List<LogicalConnectionImpl> waitingList = 
		new LinkedList<LogicalConnectionImpl>();
	
	private Object cs;
	
	//
	private LogicalConnectionPool logicalConnectionPool;

	public GarbageCollector(LogicalConnectionPool logicalConnectionPool) {
		Utils.nullArgCheck(logicalConnectionPool, "logicalConnectionPool");
		this.logicalConnectionPool = logicalConnectionPool;

		cs = new Object();
	}

	public void activate() {
		activate(false);
	}

	public void activate(boolean killAll) {
	    synchronized(cs) {
	    	Iterator<LogicalConnectionImpl> it = waitingList.iterator();

		    LogicalConnectionImpl logicalConnection;
		    while(it.hasNext()) {
		        logicalConnection = it.next();
		        if (killAll || logicalConnection.canDelete()) {
					logicalConnectionPool.returnObject(logicalConnection);
		            it.remove();
		        }
		    }
	    }
	}

	public void addDisposableClient(LogicalConnectionImpl logicalConnection) {
		//See if we can immediately remove this : 
		if (logicalConnection.canDelete()) {
			logicalConnectionPool.returnObject(logicalConnection);
			return;
		}
		//Else defer that to a later time when no one is referencing it.
	    synchronized(cs) {
		    waitingList.add(logicalConnection);
	    }
	}
}
