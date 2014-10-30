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

	/**
	 * @see {@link ServerImpl#createListener(int, ListenerOptions)}
	 * @param port
	 * @param options
	 * @return
	 */
	public boolean createListener(int port, ListenerOptions options) {
	    return impl.createListener(port, options);
	}

	/**
	 * @see {@link ClientFactory#addIPRangeAccess(String, String, boolean)}
	 * @param ipStart
	 * @param ipStop
	 */
	public void addBlockedIPRange(String ipStart, String ipStop) {
		impl.getClientFactory().addIPRangeAccess(ipStart, ipStop, false);
	}

	/**
	 * @see {@link ClientFactory#addIPRangeAccess(String, String, boolean)}
	 * @param ipStart
	 * @param ipStop
	 */
	public void addPermitIPRange(String ipStart, String ipStop){
		impl.getClientFactory().addIPRangeAccess(ipStart, ipStop, true);
	}

	/**
	 * @see {@link ServerImpl#registerService(Service)}
	 */
	public void registerService(Service service) {
	    service.setServer(this);
	    impl.registerService(service);
	}
	
	/**
	 * @see {@link ServerImpl#setServerInfos(String)}
	 * @param serverName
	 */
	public void setServerInfos(String serverName) {
	    impl.setServerInfos(serverName);
	}

	/**
	 * @see {@link ServerImpl#start(boolean)}
	 * @return
	 */
	public boolean start() {
		return start(false);
	}

	/**
	 * @see ServerImpl#start(boolean)}
	 * @param startInSeparateThread
	 * @return
	 */
	public boolean start(boolean startInSeparateThread) {
	    return impl.start(startInSeparateThread);
	}

	/**
	 * @see {@link ServerImpl#stop()}
	 */
	public void stop() {
	    impl.stop();
	}

	/**
	 * @see {@link ServerImpl#pause()}
	 */
	public void pause(){
	    impl.pause();
	}

	/**
	 * @see {@link ServerImpl#resume()}
	 */
	public void resume(){
	    impl.resume();
	}

	/**
	 * @see {@link ServerImpl#setMessageFactory(MessageFactory)}
	 * @param messageFactory
	 */
	public void setMessageFactory(MessageFactory messageFactory) {
		impl.setMessageFactory(messageFactory);
	}

	/**
	 * @see {@link ServerImpl#createQueue(String, QueueOptions)}
	 * @param queueName
	 * @param queueOptions
	 */
	public void createQueue(String queueName, QueueOptions queueOptions) {
		impl.createQueue(queueName, queueOptions);
	}

	/**
	 * @see {@link ServerImpl#removeQueue(String)}
	 * @param queueName
	 */
	public void removeQueue(String queueName) {
		impl.removeQueue(queueName);
	}

	/**
	 * @see {@link ServerImpl#pushPacket(OutgoingPacket, String)}
	 * @param packet
	 * @param queueName
	 * @return
	 */
	public boolean pushPacket(OutgoingPacket packet, String queueName) {
		return impl.pushPacket(packet, queueName);
	}

	/**
	 * @see {@link ServerImpl#pushPacket(OutgoingPacket, String, String, int)}
	 * @param packet
	 * @param queueName
	 * @param killKey
	 * @param objectCategory
	 * @return
	 */
	public boolean pushPacket(OutgoingPacket packet, String queueName, 
			String killKey, int objectCategory) {
		return impl.pushPacket(packet, queueName, killKey, objectCategory);
	}

	/**
	 * @see {@link ServerImpl#removePacketFromQueue(String, int, String)}
	 * @param killKey
	 * @param objectCategory
	 * @param queueName
	 */
	public void removePacketFromQueue(String killKey, int objectCategory, 
			String queueName) {
		impl.removePacketFromQueue(killKey, objectCategory, queueName);
	}

	/**
	 * This is extension for remote control. You could add some command
	 * processing here.
	 * @param command  the command within the request of monitor service
	 * @return the output
	 */
	public String handleMonitorRequest(String command) {
		return null;
	}
		
	public OutgoingPacket getChallenge(ConnectionContext connectionContext) {
		return null;
	}

	/**
	 * This is call back method called after the server is 
	 * started successfully.
	 */
	public void onStarted() {}

	/**
	 * Create a <code>ConnectionContext</code> object by the extended
	 * class of <code>ConnectionContext</code>.
	 * 
	 * @return  the <code>ConnectionContext</code> object
	 */
	public ConnectionContext createConnectionContext() { return null; }
	
	/**
	 * Get the <code>ServerImpl</code> object attached.
	 * @return the <code>ServerImpl</code> object
	 */
	public ServerImpl getImpl() { return impl; }
	
	/**
	 * Create a <code>LogicalConnection</code> object here.
	 * <code>LogicalConnection</code> is abstract class, so when implementing
	 * your own server, you need to extend <code>LogicalConnection</code>
	 * class. And usually when implementing this method, it is using the 
	 * extended class to create <code>LogicalConnection</code> object.
	 * @return
	 */
	public abstract LogicalConnection createLogicalConnection();

}
