package org.push.core;

import org.push.protocol.Protocol;
import org.push.util.Utils;

/**
 * Facade of <code>ListenerImpl</code>
 * 
 * @author Lei Wang
 */

public abstract class Listener {

	protected ServerImpl serverImpl;
	private ListenerImpl impl;

	private int uPort;

	protected ListenerOptions listenerOptions;

	public Listener(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
		impl = new ListenerImpl(this);
	    uPort = 0;
	    listenerOptions = new ListenerOptions();
	}
	
	public ServerImpl getServerImpl() { return serverImpl; }
	
	public int getListeningPort() { return this.uPort; }
	
	public void setListeningPort(int uPort) {
		Utils.unsignedIntArgCheck(uPort, "uPort");
		this.uPort = uPort;
	}

	public boolean startListening() {
		return impl.startListening();
	}

	public void stopListening() {
		impl.stopListening();
	}

	public void setOptions(ListenerOptions listenerOptions) {
		Utils.nullArgCheck(listenerOptions, "listenerOptions");
		this.listenerOptions = listenerOptions;
	}
	
	public ListenerOptions getOptions() {
		return listenerOptions;
	}

	public Protocol getProtocol() { return listenerOptions.getProtocol(); }
	
	protected abstract boolean handleAcceptedSocket(
			PushClientSocket clientSocket);
}
