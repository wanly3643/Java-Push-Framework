package org.push.core;

import org.push.core.Common.DisconnectionReason;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.core.Common.SendResult;
import org.push.protocol.IncomingPacket;
import org.push.protocol.OutgoingPacket;
import org.push.util.Utils;

/**
 * Facade of <code>LogicalConnectionImpl</code>
 * 
 * @author Lei Wang
 */

public abstract class LogicalConnection {

	private LogicalConnectionImpl impl;

	public LogicalConnection(ServerImpl serverImpl) {
		impl = new LogicalConnectionImpl(this, serverImpl);
	}

	public SendResult pushPacket(OutgoingPacket packet) {
		return impl.pushPacket(packet);
	}

	public SendResult tryPushPacket(OutgoingPacket packet) {
		return impl.tryPushPacket(packet);
	}

	public void disconnect(boolean waitForPendingPackets) {
		impl.disconnect(waitForPendingPackets);
	}
		
	public double getVisitDuration() {
		return impl.getVisitDuration();
	}

	public boolean subscribeToQueue(String queueName) {
		return subscribeToQueue(queueName, false);
	}

	public boolean subscribeToQueue(String queueName, 
			boolean ignorePreviousPackets) {
		return impl.subscribeToQueue(queueName, ignorePreviousPackets);
	}

	public void unSubscribeFromQueue(String queueName) {
		impl.unSubscribeFromQueue(queueName);
	}

	public void unSubscribeFromAll() {
		impl.unSubscribeFromAll();
	}

	public void incrementUsage() {
		impl.incrementUsage();
	}

	public void decrementUsage(){
		impl.decrementUsage();
	}

	public LogicalConnectionImpl getImpl() {
		return this.impl;
	}

	public void handleRequest(IncomingPacket request) {
		//
	}

	protected boolean isInactive() {
		return impl.isInactive();
	}

	protected void onReadyForSend(int nAvailableSpace) {
		Utils.unsignedIntArgCheck(nAvailableSpace, "nAvailableSpace");
	}
	
	protected void onDisconnected(DisconnectionReason closeReason) {
		//
	}

	protected void onConnected() {
		//
	}

	protected boolean onBeforeIdleClose() {
		return false;
	}

	protected abstract void recycle();

	protected abstract Login processLogin(LoginData loginData);
}
