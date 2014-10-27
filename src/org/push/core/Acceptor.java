package org.push.core;

import org.push.util.Debug;

/**
 * An <code>Acceptor</code> is the listener for common client.
 * (Another is <code>MonitorAcceptor</code> for monitor service)
 * 
 * @author Lei Wang
 */

public class Acceptor extends Listener {

	public Acceptor(ServerImpl serverImpl) {
		super(serverImpl);
	}

	/**
	 * Handle the new accepted socket
	 */
	@Override
	protected boolean handleAcceptedSocket(PushClientSocket clientSocket) {
	    //
		ClientFactory clientFactoryImpl = serverImpl.getClientFactory();
		ServerStats stats = serverImpl.getServerStats();
		ServerOptions options = serverImpl.getServerOptions();

	    stats.addToCumul(ServerStats.Measures.VisitorsSYNs, 1);

		if (clientFactoryImpl.getClientCount() >= options.getMaxConnections()) {
			Debug.debug("Reach maximum clients allowed, deny it");
			return false;
		}

		//Check if IP is allowed :
		if (!clientFactoryImpl.isAddressAllowed(clientSocket.getIP())) {
			Debug.debug("IP refused: " + clientSocket.getIP());
			return false;
		}

		return clientFactoryImpl.createPhysicalConnection(clientSocket, 
				false, listenerOptions);
	}

}
