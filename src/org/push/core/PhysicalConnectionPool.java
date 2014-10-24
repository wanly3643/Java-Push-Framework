package org.push.core;

import org.push.protocol.AbstractPool;
import org.push.util.Utils;


/**
 * Pool to manage <code>PhysicalConnection</code>
 * 
 * @author Lei Wang
 */

public class PhysicalConnectionPool extends AbstractPool<PhysicalConnection> {

	private ServerImpl serverImpl;
	
	public PhysicalConnectionPool(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	}

	@Override
	protected void deleteImpl(PhysicalConnection connection) {
	}

	@Override
	protected PhysicalConnection createImpl() {
		return new PhysicalConnection(serverImpl);
	}

	@Override
	protected void recycleObject(PhysicalConnection connection) {
		connection.recycle();
	}

}
