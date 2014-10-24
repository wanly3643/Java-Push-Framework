package org.push.monitor;

import org.push.core.LogicalConnectionImpl;
import org.push.core.ServerImpl;

/**
 * 
 * @author Lei Wang
 */

public class MonitorConnection extends LogicalConnectionImpl {

	private String monitorKey;

	public MonitorConnection(String key, ServerImpl serverImpl) {
		super(null, serverImpl);
		monitorKey = key;
	}
	
	public String getKey() { return monitorKey; }

	public boolean isMonitor() { return true; }

}
