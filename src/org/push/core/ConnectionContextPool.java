package org.push.core;

import org.push.protocol.AbstractPool;
import org.push.util.Utils;

/**
 * Pool to manage <code>ConnectionContext</code>
 * 
 * @author Lei Wang
 */

public class ConnectionContextPool extends AbstractPool<ConnectionContext> {

	private ServerImpl serverImpl;
	
	public ConnectionContextPool(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	}

	@Override
	protected void deleteImpl(ConnectionContext context) {
	}

	@Override
	protected ConnectionContext createImpl() {
		return serverImpl.getFacade().createConnectionContext();
	}

	@Override
	protected void recycleObject(ConnectionContext context) {
		context.recycle();
	}
}
