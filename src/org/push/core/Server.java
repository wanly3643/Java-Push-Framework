package org.push.core;

import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;

/**
 * Facade of the <code>ServerImpl</code>.
 * 
 * @author Lei Wang
 */

public abstract class Server {

	private ServerImpl impl;

	public Server() {
	    impl = new ServerImpl(this);
	}

	public boolean createListener(int port, ListenerOptions options) {
	    return impl.createListener(port, options);
	}

	public void addBlockedIPRange(String ipStart, String ipStop) {
		impl.getClientFactory().addIPRangeAccess(ipStart, ipStop, false);
	}

	public void addPermitIPRange(String ipStart, String ipStop){
		impl.getClientFactory().addIPRangeAccess(ipStart, ipStop, true);
	}

	public void registerService(Service service) {
	    service.setServer(this);
	    //
	    impl.registerService(service);
	}
	
	public void setServerInfos(String serverName) {
	    impl.setServerInfos(serverName);
	}

	public String handleMonitorRequest(String command) {
		return null;
	}

	public boolean start() {
		return start(false);
	}

	public boolean start(boolean startInSeparateThread) {
	    return impl.start(startInSeparateThread);
	}

	public void stop() {
	    impl.stop();
	}

	public void pause(){
	    impl.pause();
	}

	public void resume(){
	    impl.resume();
	}

	public void setMessageFactory(MessageFactory messageFactory) {
		impl.setMessageFactory(messageFactory);
	}

	public void createQueue(String queueName, QueueOptions queueOptions) {
		impl.createQueue(queueName, queueOptions);
	}

	public void removeQueue(String queueName) {
		impl.removeQueue(queueName);
	}

	public boolean pushPacket(OutgoingPacket packet, String queueName) {
		return impl.pushPacket(packet, queueName);
	}

	public boolean pushPacket(OutgoingPacket packet, String queueName, 
			String killKey, int objectCategory) {
		return impl.pushPacket(packet, queueName, killKey, objectCategory);
	}

	public void removePacketFromQueue(String killKey, int objectCategory, 
			String queueName) {
		impl.removePacketFromQueue(killKey, objectCategory, queueName);
	}
		
	public OutgoingPacket getChallenge(ConnectionContext connectionContext) {
		return null;
	}

	public void onStarted() {}

	public ConnectionContext createConnectionContext() { return null; }
	
	public ServerImpl getImpl() { return impl; }
	
	/**
	 * Create a <code>LogicalConnection</code> object here.
	 * <code>LogicalConnection</code> is abstract class, so when implementing
	 * your own server, you need to extend <code>LogicalConnection</code>
	 * class. And usually when implementing this method, it is using the 
	 * extended class to create object.
	 * @return
	 */
	public abstract LogicalConnection createLogicalConnection();

}
