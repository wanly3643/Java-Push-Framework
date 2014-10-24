package org.push.core;

import java.util.HashMap;
import java.util.Map;

import org.push.core.Common.DisconnectionReason;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.impl.xml.XMLPacket;
import org.push.monitor.AnalyticsProtocol;
import org.push.monitor.MonitorConnection;
import org.push.monitor.MonitorsBroadcastManager;
import org.push.monitor.MonitorsMsgFactory;
import org.push.protocol.DeserializeData;
import org.push.protocol.ErrorCodes.NetworkDeserializeResult;
import org.push.protocol.IncomingPacket;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolManager;
import org.push.protocol.RecyclableBuffer;
import org.push.util.Ptr;
import org.push.util.StopWatch;
import org.push.util.Utils;

/**
 * A <code>Dispatcher</code> holds a lots of <code>Service</code>.
 * It will use the registered service to process the request and 
 * produce the response.
 * 
 * @author Lei Wang
 */

public class Dispatcher {

	//List of services :
	private Map<Integer, Service> serviceMap = new HashMap<Integer, Service>();

	//Reference to dispatched services :
	private Map<Long, String> workerServiceMap = new HashMap<Long, String>();
	private Object csSrvMap;
	
	// 
	private ServerImpl serverImpl;
	
	public Dispatcher(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
		
		this.csSrvMap = new Object();
	}
	
	/**
	 * Initialize a new created <code>PhysicalConnection</code>
	 * 
	 * @param connection  new created <code>PhysicalConnection</code>
	 */
	public void handleInitialize(PhysicalConnection connection) {
		RecyclableBuffer protocolBytes = new RecyclableBuffer();
		Protocol protocol = connection.advanceInitialization(protocolBytes);
		if (protocol != null) {
			connection.pushBytes(protocolBytes, protocol.getLowerProtocol());
		}

		connection.checkConnectionInitialization();

	    connection.postReceive();
	    
	    protocolBytes.release();
	}

	/**
	 * Handle the read event. (When there is data from the client)
	 * 
	 * @param connection
	 * @param dwIoSize   reserved, usually -1 is given
	 */
	public void handleRead(PhysicalConnection connection, int dwIoSize) {
		ClientFactory clientFactoryImpl = serverImpl.getClientFactory();
		ProtocolManager theProtocolManager = serverImpl.getProtocolManager();
		
		PhysicalConnection.Status status = connection.getStatus();

		boolean isFailed = false;
		boolean waitForPendingPackets = false;

		RecyclableBuffer incomingBytes = new RecyclableBuffer(
				RecyclableBuffer.Type.UnAllocated);
		isFailed = !connection.readReceivedBytes(incomingBytes, dwIoSize);

		if (!isFailed) {
			isFailed = !theProtocolManager.processIncomingData(connection, 
					incomingBytes);
		}

		//The Processing Loop.
		DeserializeData deserializeData = new DeserializeData(
				connection.getProtocol().getLowestProtocol());
		NetworkDeserializeResult result;
		while (!isFailed) {
			deserializeData.clear();
			result = theProtocolManager.tryDeserializeIncomingPacket(
					connection, deserializeData);
			
			if (result == NetworkDeserializeResult.ProtocolBytes) {	
				if (deserializeData.getProtocolBytes().hasBytes()) {
					connection.pushBytes(deserializeData.getProtocolBytes(), 
							deserializeData.getProtocol().getLowerProtocol());
				}

				continue;
			}
			else if (result == NetworkDeserializeResult.Close)
			{
				if (deserializeData.getProtocolBytes().hasBytes()) {
					connection.pushBytes(deserializeData.getProtocolBytes(), 
							deserializeData.getProtocol().getLowerProtocol());
				}

				isFailed = true;
				waitForPendingPackets = true;
			}
			else if (result == NetworkDeserializeResult.Content)
			{
				handleRequest(connection, deserializeData.getMessage(), 
						deserializeData.getRoutingService(), 0);
				connection.getMessageFactory().disposeIncomingPacket(
						deserializeData.getMessage());
				break;
			}
			else if(result == NetworkDeserializeResult.WantMoreData) {
				connection.postReceive();
				break;
			} else {
				/*result == NetworkDeserializeResult::Failure or 
				NetworkDeserializeResult::Initializationfailure*/
				isFailed = true;
			}		
		}
		
		// Check if the socket is closed
		if (!isFailed) {
			isFailed = !connection.getSocket().isConnected();
		}

		if (!isFailed) {
			connection.checkConnectionInitialization();
			connection.postReceive();
		} else {
			if (status == PhysicalConnection.Status.Attached) {
				clientFactoryImpl.disconnect(
						connection.getLogicalConnectionImpl(), 
						waitForPendingPackets, 
						DisconnectionReason.PeerClosure);
			} else {
				connection.closeConnection(waitForPendingPackets);
			}
		}

		connection.decrementIoWorkersReferenceCounter();
		
		deserializeData.release();		
		incomingBytes.release();
	}
	
	/**
	 * Handle the write event (when there is data to send to the client)
	 * 
	 * @param connection
	 * @param dwIoSize   reserved, usually -1 is given
	 */
	public void handleWrite(PhysicalConnection connection, int dwIoSize) {
		ClientFactory clientFactoryImpl = serverImpl.getClientFactory();

		boolean bIsBufferIdle = false;
		PhysicalConnection.Status status = connection.getStatus();
		Ptr<Boolean> pIsBufferIdle = new Ptr<Boolean>(Boolean.FALSE);
		if (!connection.onSendCompleted(pIsBufferIdle)) {
			if (status == PhysicalConnection.Status.Attached) {
				clientFactoryImpl.disconnect(
						connection.getLogicalConnectionImpl(), 
						false, DisconnectionReason.PeerClosure);
			} else {
				connection.closeConnection(false);
			}

			connection.decrementIoWorkersReferenceCounter();
			return;
		}
		
		if (pIsBufferIdle.notNull()) {
			bIsBufferIdle = pIsBufferIdle.get().booleanValue();
		}

		if (status == PhysicalConnection.Status.Attached) {
			if (bIsBufferIdle) {
				connection.getLogicalConnectionImpl().
					checkAndProcessPendingBroadcast(false);
			}

			if (!connection.isObserverChannel()) {
				connection.getLogicalConnectionImpl().onReadyForSend(
						connection.getSendBuffer().getRemainingSize());
			}
		}

		connection.decrementIoWorkersReferenceCounter();
	}

	/**
	 * Handle when there is failure on the socket I/O
	 * @param connection
	 */
	public void handleFailedIO(PhysicalConnection connection) {
		connection.decrementIoWorkersReferenceCounter();
	}

	/**
	 * Register the service by the routing id
	 * @see Service#getRoutingId()
	 * 
	 * @param service
	 */
	public void registerService(Service service) {
	    serviceMap.put(service.getRoutingId(), service);
	}

	/**
	 * Get all the services' name by XML format
	 * @return
	 */
	public String getServiceNames() {
	    StringBuilder sb = new StringBuilder();

	    for (Service s : serviceMap.values()) {
	    	sb.append("<service val=\"");
	    	sb.append(s.getName());
	    	sb.append("\"/>");
	    }

	    return sb.toString();
	}

	/**
	 * Set the service for current thread
	 * 
	 * @param serviceName name of the service
	 */
	public void setCurrentService(String serviceName) {
	    synchronized(csSrvMap) {
	    	Long dwThread = new Long(Thread.currentThread().getId());
		    workerServiceMap.put(dwThread, serviceName);
	    }
	}
	
	/**
	 * Remove the service of current thread
	 */
	public void unsetCurrentService() {
	    synchronized(csSrvMap) {
	    	Long dwThread = new Long(Thread.currentThread().getId());
		    workerServiceMap.remove(dwThread);
	    }
	}

	/**
	 * Get the service of current thread
	 */
	public String getCurrentService() {
	    synchronized(csSrvMap) {
	    	Long dwThread = new Long(Thread.currentThread().getId());
		    return workerServiceMap.get(dwThread);
	    }
	}

	/**
	 * Send a message to the clients of monitor service that
	 * there is a client arrive
	 * 
	 * @param key      Unique name of the incoming client
	 * @param peerIP   IP of the incoming client
	 * @param peerPort Port of the incoming client
	 */
	public void notifyObserversClientIN(String key, String peerIP, 
			int peerPort) {
		Utils.unsignedIntArgCheck(peerPort, "peerPort");
		ServerOptions options = serverImpl.getServerOptions();
		MonitorsBroadcastManager monitorBroadcastManager = 
			serverImpl.getMonitorsBroadcastManager();

		if (!options.isProfilingEnabled()) {
			return;
		}

	    String timestamp = Utils.getCurrentTime();

	    XMLPacket response = new XMLPacket(AnalyticsProtocol.VisitorInResponse);
	    response.setArgumentAsText("time", timestamp);
	    response.setArgumentAsText("name", key);
	    response.setArgumentAsText("ip", peerIP);
	    response.setArgumentAsInt("port", peerPort);

	    monitorBroadcastManager.pushPacket(response, "clientsIn", key, 0);
	}


	/**
	 * Send a message to the clients of monitor service that
	 * there is a client leave
	 * 
	 * @param key  Unique name of the leaving client
	 */
	public void notifyObserversClientOut(String key) {
		ServerOptions options = serverImpl.getServerOptions();
		MonitorsBroadcastManager monitorBroadcastManager = 
			serverImpl.getMonitorsBroadcastManager();

		if (!options.isProfilingEnabled()) {
			return;
		}
	    
		XMLPacket response = new XMLPacket(
				AnalyticsProtocol.VisitorOutResponse);
	    response.setArgumentAsText("name", key);

	    monitorBroadcastManager.pushPacket(response, "clientsOut");

	    //Remove client from the other broadcast group :
	    monitorBroadcastManager.removePacket(key, 0, "clientsIn");
	}


	private void handleRequest(PhysicalConnection connection, 
			IncomingPacket packet, int nRoutingService, int serviceBytes) {
		Utils.unsignedIntArgCheck(serviceBytes, "serviceBytes");

		if (connection.getStatus() == PhysicalConnection.Status.Connected) {
			if (!connection.isObserverChannel()) {
				processFirstRequest(connection, packet, nRoutingService, 
						serviceBytes);
			} else {
				processMonitorFirstRequest(connection, packet);
			}

			return;
		}

		dispatchRequest(connection, packet, nRoutingService, serviceBytes);
	}

	private void dispatchRequest(PhysicalConnection connection, 
			IncomingPacket packet, int nRoutingService, int serviceBytes ) {
		Utils.unsignedIntArgCheck(serviceBytes, "serviceBytes");
		ServerStats stats = serverImpl.getServerStats();

		if (connection.getStatus() != PhysicalConnection.Status.Attached) {
			return;
		}

		if (connection.isObserverChannel()) {
			handleMonitorRequest(connection, packet);
			return;
		}

		LogicalConnectionImpl client = connection.getLogicalConnectionImpl();

		//
		Service service = serviceMap.get(new Integer(nRoutingService));
		if (service == null) {
			client.getFacade().handleRequest(packet);
			return;
		}
		//

		setCurrentService(service.getName());

		StopWatch watch = new StopWatch();
		service.handle(client.getFacade(), packet);

		double duration = watch.getElapsedTime();
		stats.addToDistribution(
				ServerStats.Measures.PerformanceProcessingTimePerService, 
				service.getName(), duration);

		stats.addToDuration(ServerStats.Measures.PerformanceProcessingTime, 
				duration);

		unsetCurrentService();

		//Stats. :
		stats.addToDistribution(
				ServerStats.Measures.BandwidthInboundVolPerRequest, 
				service.getName(), serviceBytes);
		stats.addToDistribution(
				ServerStats.Measures.PerformanceRequestVolPerRequest, 
				service.getName(), 1);
		
	}

	private void processFirstRequest(PhysicalConnection connection, 
			IncomingPacket packet, int nRoutingService, int serviceBytes) {
		Utils.unsignedIntArgCheck(serviceBytes, "serviceBytes");
	    //OutgoingPacket pOutPacket = null;
		LogicalConnectionPool logicalConnectionPool = 
			serverImpl.getLogicalConnectionPool();
		ServerStats stats = serverImpl.getServerStats();
		ClientFactory clientFactoryImpl = serverImpl.getClientFactory();


		LoginData loginData = new LoginData();
		loginData.setConnectionContext(connection.getConnectionContext());
		loginData.setRequest(packet);

		LogicalConnectionImpl newClient = logicalConnectionPool.borrowObject();

		Login type = newClient.processLogin(loginData);

		if (!Login.IsSucceeded(type)) {
			boolean bWaitForSendToComplete = (loginData.getResponse() != null);
			if (loginData.getResponse() != null) {
				connection.pushPacket(loginData.getResponse());
				serverImpl.getMessageFactory().disposeOutgoingPacket(
						loginData.getResponse());
			}

			if (type == Login.RefuseAndClose) {
				connection.closeConnection(bWaitForSendToComplete);
			}

			logicalConnectionPool.returnObject(newClient);
			return;
		}

		//At this moment, claim the physical connection:
		if (!clientFactoryImpl.removePhysicalConnection(connection)) {
			logicalConnectionPool.returnObject(newClient);
			return;
		}

		newClient.initializeIntern(connection);

		serverImpl.addClientToStreamers(newClient);

		//type == Login::AcceptClient.
		clientFactoryImpl.addLogicalConnection(newClient);

		newClient.onConnected();

		if (type == Login.AcceptClientAndRouteRequest) {
			dispatchRequest(connection, loginData.getRequest(), 
					nRoutingService, serviceBytes);
		}
		
		//Statistics :
		stats.addToCumul(ServerStats.Measures.VisitorsHitsIn, 1);
		//notifyObserversClientIN(addedClient->getKey(), connection.getPeerIP(), connection.getPeerPort());
		stats.addToCumul(ServerStats.Measures.VisitorsOnline, 1);
	    

	    newClient.decrementUsage();
	}

	private void handleMonitorRequest(PhysicalConnection connection, 
			IncomingPacket packet) {
		ServerOptions options = serverImpl.getServerOptions();
		ServerStats stats = serverImpl.getServerStats();
		MonitorsMsgFactory theMonitorsMsgFactory = 
			MonitorsMsgFactory.getDefault();

	    XMLPacket requestPacket = (XMLPacket) packet;

	    AnalyticsProtocol typeId = requestPacket.getTypeId();

	    if (typeId == AnalyticsProtocol.LiveSubscriptionRequest) {
	        boolean bSubscribe = requestPacket.getArgumentAsBool("resume");
	        if (bSubscribe) {
	            OutgoingPacket initPacket = stats.getInitializationPacket();
	            connection.pushPacket(initPacket);
				theMonitorsMsgFactory.disposeOutgoingPacket(initPacket);

	            //
	/*
	            monitorBroadcastManager.subscribeClient(connection->getLogicalConnectionKey(), "stats");
	            monitorBroadcastManager.subscribeClient(connection->getLogicalConnectionKey(), "clientsIn");
	            monitorBroadcastManager.subscribeClient(connection->getLogicalConnectionKey(), "clientsOut");
	*/
	        } else {
	            //monitorBroadcastManager.removeClient(connection->getLogicalConnectionKey());
	        }
	    }

	    if (typeId == AnalyticsProtocol.LogoutRequest) {
	        connection.closeConnection(false);
	    }

	    if (typeId == AnalyticsProtocol.ConsoleCommandRequest) {
	        String command = requestPacket.getArgumentAsText("command");

	        XMLPacket response = new XMLPacket(
	        		AnalyticsProtocol.ConsoleCommandResponse);

	        response.setArgumentAsText("client", 
	        		requestPacket.getArgumentAsText("client"));

	        if ("about".equals(command)) {
	            String str = serverImpl.getServerInfos() + 
	            	"\nBased on PushFramework version 1.0";
	            response.setArgumentAsText("console", str);
	        } else if ("profiling enable".equals(command)) {
	        	String str;
	            if (options.isProfilingEnabled()) {
	                str = "Profiling is already enabled.";
	            } else {
	                options.setProfilingEnabled(true);
	                str = "Profiling was enabled.";
	            }
	            response.setArgumentAsText("console", str);
	        } else if ("profiling disable".equals(command)) {
	        	String str;
	            if (!options.isProfilingEnabled()) {
	                str = "Profiling is already disabled.";
	            } else {
	                options.setProfilingEnabled(false);
	                str = "Profiling was disabled.";
	            }
	            response.setArgumentAsText("console", str);
	        } else if ("profiling status".equals(command)) {
	            response.setArgumentAsText("console", 
	            		(options.isProfilingEnabled()) ? 
	            				"Profiling was found to be enabled." :
	                            "Profiling was found to be disabled.");
	        }

	        String out = serverImpl.getFacade().handleMonitorRequest(command);

	        if (out != null) {
	            response.setArgumentAsText("console", out);
	        }

	        connection.pushPacket(response);
	    }
	}

	private void processMonitorFirstRequest(PhysicalConnection connection, 
			IncomingPacket packet) {
	    XMLPacket requestPacket = (XMLPacket) packet;
		ServerOptions options = serverImpl.getServerOptions();
		ClientFactory clientFactoryImpl = serverImpl.getClientFactory();

	    //
	    XMLPacket response = new XMLPacket(AnalyticsProtocol.LoginResponse);
	    String password = requestPacket.getArgumentAsText("password");

		if (options.getPassword() != null && 
				!options.getPassword().equals(password)) {
			response.setArgumentAsText("result", "pass");
			connection.pushPacket(response);
			return;
		}

		if (!clientFactoryImpl.removePhysicalConnection(connection)) {
			return;
		}

		int observerId = 1; // TODO C++ uses static here
		observerId ++;

		MonitorConnection monitor = new MonitorConnection(
				String.valueOf(observerId), serverImpl);
		monitor.initializeIntern(connection);

		//Add client to a streamer :
		serverImpl.addClientToStreamers(monitor);

		response.setArgumentAsText("result", "ok");
		connection.pushPacket(response);

		monitor.decrementUsage();
	}
}
