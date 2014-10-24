package org.push.monitor;

import org.push.core.Listener;
import org.push.core.PushClientSocket;
import org.push.core.ServerImpl;
import org.push.impl.xml.XMLProtocol;

/**
 * 
 * @author Lei Wang
 */

public class MonitorAcceptor extends Listener {

	private XMLProtocol protocol;
	
	public MonitorAcceptor(ServerImpl serverImpl) {
		super(serverImpl);
		protocol = new XMLProtocol();

	    listenerOptions.setIsTcpNoDelay(true);
	    listenerOptions.setInterfaceAddress(null);
	    listenerOptions.setListeningBackLog(10);
	    listenerOptions.setProtocol(protocol);
	    listenerOptions.setSynAttackPrevention(false);
	}

	@Override
	protected boolean handleAcceptedSocket(PushClientSocket clientSocket) {
		return serverImpl.getClientFactory().createPhysicalConnection(
				clientSocket, true, listenerOptions);
	}

}
