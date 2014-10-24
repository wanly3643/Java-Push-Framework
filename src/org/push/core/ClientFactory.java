package org.push.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.push.core.Common.DisconnectionReason;
import org.push.exception.InvalidIPAddressException;
import org.push.util.Debug;
import org.push.util.IPRange;
import org.push.util.Utils;

/**
 * A <code>ClientFactory</code> is used to manage the connections.
 * And it will provide the features as below:
 * 1) Add a new connection
 * 2) Find an existing connection
 * 3) Disconnect an existing connection
 * 
 * @author Lei Wang
 */

public class ClientFactory {

	/* To store the common connections */
	private Set<LogicalConnectionImpl> clientSet = 
		new HashSet<LogicalConnectionImpl>();
	private int nClientsCount;

	/* To store the connections of monitor service */
	private Set<LogicalConnectionImpl> monitorSet = 
		new HashSet<LogicalConnectionImpl>();
	
	/**
	 * Lock for {@link #clientSet}, {@link #nClientsCount}
	 * and {@link #monitorSet}
	 */
	private Object cs;

	/**
	 * Lock for {@link #vectPendingPhysicalConnections}
	 */
	private Object csChannelMap;
	private List<PhysicalConnection> vectPendingPhysicalConnections = 
		new LinkedList<PhysicalConnection>();

	private List<IPRange> blockedIPs = new LinkedList<IPRange>();
	private List<IPRange> permittedIPs = new LinkedList<IPRange>();

	/**
	 * If true, it will check if the IP is within the {@link #permittedIPs}
	 * to determine if the IP is allowed.
	 * Otherwise, it will only if IP is within the {@link #blockedIPs}
	 * to determine if the IP is blocked or not.
	 */
	private boolean isPermitOnly;
	
	// Common objects
	private ServerImpl serverImpl;

	public ClientFactory(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
		
	    nClientsCount = 0;
	    cs = new Object();
	    csChannelMap = new Object();
		isPermitOnly = false;
	}

	public void addLogicalConnection(LogicalConnectionImpl logicalConnection) {
		synchronized(cs) {
		    //
			if (logicalConnection.isMonitor()) {
				monitorSet.add(logicalConnection);
			} else {
				nClientsCount += 1;
				clientSet.add(logicalConnection);
			}
	
			logicalConnection.incrementUsage();
		}
	}
	    
	public boolean disconnect(LogicalConnectionImpl logicalConnection, 
			boolean waitForPendingPackets, DisconnectionReason closeReason) {
		if (!removeIfExisting(logicalConnection)) {
	        return false;
	    }

		disconnectIntern(logicalConnection, waitForPendingPackets, closeReason);
		return true;
	}
	    
	public void disconnectIntern(LogicalConnectionImpl logicalConnection, 
			boolean waitForPendingPackets, DisconnectionReason closeReason) {
		// Close the physical connection first
		logicalConnection.getPhysicalConnection().closeConnection(
				waitForPendingPackets);

		GarbageCollector garbageCollector = serverImpl.getGarbageCollector();
		ServerStats stats = serverImpl.getServerStats();

		// Save some logs for common connections
		if (!logicalConnection.isMonitor()) {
			//dispatcher.notifyObserversClientOut(pLogicalConnection->getKey());

			//Statistics :
			stats.addToCumul(ServerStats.Measures.VisitorsHitsOut, 1);
			stats.addToCumul(ServerStats.Measures.VisitorsOnline, -1);
			stats.addToDuration(ServerStats.Measures.VisitorsDuration, 
					logicalConnection.getVisitDuration());

			logicalConnection.onDisconnected(closeReason);
		}
		
		/* Remove from the broadcast list */
		logicalConnection.getStreamer().removeItem(logicalConnection);
		
		/* Do garbage collection */
		garbageCollector.addDisposableClient(logicalConnection);
	}

	public void returnClient(LogicalConnectionImpl client) {
	    client.decrementUsage();
	}

	public int getClientCount() { return nClientsCount; }
	
	/**
	 * Scan and clear the inactive connections
	 */
	public void scrutinize() {
		synchronized(cs) {
			Iterator<LogicalConnectionImpl> it = clientSet.iterator();
			LogicalConnectionImpl client;
			while (it.hasNext()) {
				client = it.next();
				if (client.isInactive()) {
					nClientsCount -= 1;
					disconnectIntern(client, client.onBeforeIdleClose(), 
							DisconnectionReason.InactiveClient);
					it.remove();
				}
			}
	
			///////////
			scrutinizeChannels();
		}
	}

	/**
	 * Scan and clear the expired connections which are pending
	 */
	public void scrutinizeChannels() {
		ServerOptions options = serverImpl.getServerOptions();
		synchronized(csChannelMap) {
			Iterator<PhysicalConnection> it = 
				vectPendingPhysicalConnections.iterator();
			
			PhysicalConnection connection;
			while (it.hasNext()) {
				connection = it.next();
	
				if (connection.getStatus() == 
						PhysicalConnection.Status.Connected) {
					// Verify expiry : 
					int nMaxDuration = connection.isObserverChannel() ? 
							40 : options.getLoginExpireDuration();
					if (connection.getLifeDuration() > nMaxDuration) {
						connection.closeConnection(false);
					}
				}
	
				// Delete disposable pending connections :
				if (connection.checkIfUnusedByIOWorkers()) {
					disposePhysicalConnection(connection);
	
					it.remove();
				}       
			}
		}
	}

	public void addIPRangeAccess(String ipStart, String ipStop, 
			boolean bPermit) {
		isPermitOnly = bPermit;

		IPRange ipRange;
		try {
			ipRange = new IPRange(ipStart, ipStop);
		} catch (InvalidIPAddressException e) {
			e.printStackTrace();
			return;
		}

		List<IPRange> list= bPermit ? permittedIPs : blockedIPs;

		list.add(ipRange);
	}

	public boolean isAddressAllowed(String ip) {
		if (!IPRange.isValidIP(ip)) {
			System.err.println("Invalid IP: " + ip);
		}

		List<IPRange> list = isPermitOnly ? permittedIPs : blockedIPs;
		boolean bIsInList = false;

		for (IPRange ipRange : list) {
			try {
				if (ipRange.isWithin(ip)) {
					bIsInList = true;
					break;
				}
			} catch (InvalidIPAddressException e) {
				e.printStackTrace();
				return false;
			}
		}
		return isPermitOnly ? bIsInList : !bIsInList;
	}

	/**
	 * Create <code>PhysicalConnection</code> for the new accepted
	 * socket and save it under management.
	 * 
	 * @param socket
	 * @param isObserver  If it is for monitor service
	 * @param listenerOptions
	 * @return
	 */
	public boolean createPhysicalConnection(PushClientSocket socket,
			boolean isObserver, ListenerOptions listenerOptions) {
		PhysicalConnectionPool thePhysicalConnectionPool = 
			serverImpl.getPhysicalConnectionPool();
		IOQueue<PhysicalConnection> ioQueue = serverImpl.getIOQueue();
		PhysicalConnection connection = 
			thePhysicalConnectionPool.borrowObject();
		// In both cases reset the object.
		connection.reset(socket, isObserver, listenerOptions);

		//
		if (!connection.setUpProtocolContexts()) {
			thePhysicalConnectionPool.returnObject(connection);
			return false;
		}

		Debug.debug("Physical Connection Created for client from: " + 
				socket.getIP());

	    //Now Associate with IOCP main Handle
	    if (!ioQueue.addSocketContext(socket, connection)) {
			thePhysicalConnectionPool.returnObject(connection);
	        //leave socket close to acceptor
	        return false;
	    }

		Debug.debug("Queue adds client from: " + socket.getIP());

		addPhysicalConnection(connection);
		 
		serverImpl.getDispatcher().handleInitialize(connection);

	    return true;
	}

	public void addPhysicalConnection(PhysicalConnection connection) {
		synchronized(csChannelMap) {
			vectPendingPhysicalConnections.add(connection);
		}
	}

	public boolean removePhysicalConnection(PhysicalConnection connection) {
		synchronized(csChannelMap) {
			return vectPendingPhysicalConnections.remove(connection);
		}
	}
	
	public void stop() {
		synchronized(csChannelMap) {
			//Remove all connections (Demux is already stopped):
			Iterator<PhysicalConnection> it = 
				vectPendingPhysicalConnections.iterator();
			while (it.hasNext()) {
				disposePhysicalConnection(it.next());
			}
			
			vectPendingPhysicalConnections.clear();
		}
	}
	
	public void disposePhysicalConnection(PhysicalConnection connection) {
		PhysicalConnectionPool thePhysicalConnectionPool = 
			serverImpl.getPhysicalConnectionPool();
		thePhysicalConnectionPool.returnObject(connection);
	}

	private boolean removeIfExisting(LogicalConnectionImpl logicalConnection) {
		synchronized(cs) {
			Set<LogicalConnectionImpl> set = logicalConnection.isMonitor() ? 
					monitorSet : clientSet;
			if (set.remove(logicalConnection)) {
				if (!logicalConnection.isMonitor()) {
					nClientsCount -= 1;
				}
	
				return true;
			}
	
			return false;
		}
	}
}
