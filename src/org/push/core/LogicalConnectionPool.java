package org.push.core;

import org.push.protocol.AbstractPool;
import org.push.util.Utils;


/**
 * Pool to manage <code>LogicalConnectionImpl</code>
 * 
 * @author Lei Wang
 */

public class LogicalConnectionPool 
		extends AbstractPool<LogicalConnectionImpl> {

	private ServerImpl serverImpl;
	
	public LogicalConnectionPool(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	}

	@Override
	protected void deleteImpl(LogicalConnectionImpl logicalConnection) {
	}

	@Override
	protected LogicalConnectionImpl createImpl() {
		LogicalConnection logicalConnection = 
			serverImpl.getFacade().createLogicalConnection();
		return logicalConnection.getImpl();
	}

	@Override
	protected void recycleObject(LogicalConnectionImpl logicalConnection) {
		logicalConnection.recycle();
	}
}
