package org.push.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.push.monitor.MonitorAcceptor;
import org.push.monitor.MonitorsBroadcastManager;
import org.push.protocol.BufferPool;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolManager;
import org.push.protocol.RecyclableBuffer;
import org.push.util.Debug;
import org.push.util.Utils;

/**
 * The real implementation of the server.
 * 
 * @author Lei Wang
 */

public class ServerImpl {

	private Server facade;
	private String serverInfos;

	// User defined
	private MessageFactory msgFactory;

	private int uGCPeriod;
	private int nSecsGCCount;
	private int nSecsPerfObservCount;
	private List<BroadcastStreamer> streamersList = 
		new LinkedList<BroadcastStreamer>();
	private BroadcastStreamer monitorsStreamer;

	private Map<Integer, Acceptor> listenersMap =
		new HashMap<Integer, Acceptor>();
	
	private Thread hThread;
	private volatile boolean isRunning;

	// Components of this server
	private BufferPool pool;
	private PhysicalConnectionPool thePhysicalConnectionPool;
	private LogicalConnectionPool theLogicalConnectionPool;
	private ConnectionContextPool theConnectionContextPool;
	private ProtocolManager theProtocolManager;
	private BroadcastManager broadcastManager;
	private ClientFactory clientFactoryImpl;
	private Dispatcher dispatcher;
	private Demultiplexor demux;
	private GarbageCollector garbageCollector;
	private IOQueue<PhysicalConnection> ioQueue;
	private ServerStats stats;
	private ServerOptions options;
	
	// Components of this server monitor
	private MonitorAcceptor monitorAcceptor;
	private MonitorsBroadcastManager monitorBroadcastManager;
	
	public ServerImpl(Server facade) {
	    this.facade = facade;

	    uGCPeriod = 10;

		monitorsStreamer = null;

	    nSecsGCCount=0;
	    nSecsPerfObservCount=0;
	    
	    //
	    initComponent();
	}

	private void eternalLoop() {
	     facade.onStarted();
	     long deadline = System.currentTimeMillis();
	     while (true) {
	    	 deadline = System.currentTimeMillis() + 1000;
	    	 LockSupport.parkUntil(deadline);
	    	 
	    	 // timeout
	    	 if (isRunning && System.currentTimeMillis() >= deadline) {
	    		 doPeriodicJobs();
	    	 } else {
	    		 break;
	    	 }
	     }
	}
	private void doPeriodicJobs() {
	    nSecsGCCount += 1;
	    nSecsPerfObservCount += 1;

	    if(nSecsGCCount == uGCPeriod) {
	        nSecsGCCount=0;
	        garbageCollector.activate();
	        clientFactoryImpl.scrutinize();
	    }

	    // Reach the sampling point, save the data of server status
	    if(nSecsPerfObservCount == options.getSamplingRate() && 
	    		options.isProfilingEnabled()) {
	        nSecsPerfObservCount=0;
	        OutgoingPacket packet = stats.getPerformancePacket();
	        monitorBroadcastManager.pushPacket(packet, "stats");
	    }
	}
	
	private boolean startListening() {
		for (Acceptor acceptor : listenersMap.values()) {
			if (!acceptor.startListening()) {
				stopListening();
				return false;
			}
		}
		return true;
	}

	private void stopListening() {
		for (Acceptor acceptor : listenersMap.values()) {
			acceptor.stopListening();
		}
	}

	private int calculateMaxBufferSizePerMessage() {
		int nMaxSize = 0;

		for (Acceptor acceptor : listenersMap.values()) {
			int nSize = msgFactory.getMaximumMessageSize();
			Protocol protocol = acceptor.getProtocol();
			while (protocol != null) {
				nSize = protocol.getRequiredOutputSize(nSize);
				protocol = protocol.getLowerProtocol();
			}

			if (nSize > nMaxSize) {
				nMaxSize = nSize;
			}
		}

		return nMaxSize;
	}
	private int calculateAdditionalBuffersForProtocols(
			int nMaxPoolConnections, int nMaxThreads) {
		Utils.unsignedIntArgCheck(nMaxPoolConnections, "nMaxPoolConnections");
		Utils.unsignedIntArgCheck(nMaxThreads, "nMaxThreads");

		int nTotal = 0;
		for (Acceptor acceptor : listenersMap.values()) {
			nTotal += acceptor.getProtocol().getRequiredRecyclableBuffers(
					nMaxPoolConnections, nMaxThreads);
		}

		return nTotal;
	}

	private boolean initializeProtocolContextPools(int nMaxPoolConnections) {
		Utils.unsignedIntArgCheck(nMaxPoolConnections, "nMaxPoolConnections");

		for (Acceptor acceptor : listenersMap.values()) {
			if (!acceptor.getProtocol().initialize(nMaxPoolConnections))
				return false;
		}

		return true;
	}
	
	private boolean initializeBufferPool(int nMaxPoolConnections) {
		int nMaxThreads = options.getWorkersCount() + 20;
		//
		int nMessageSize = calculateMaxBufferSizePerMessage();
		int nMaxAdditionalMsgBuffersForProtocols = 
			calculateAdditionalBuffersForProtocols(nMaxPoolConnections, 
					nMaxThreads);
		if (!pool.create(RecyclableBuffer.Type.Single, 
				nMaxThreads * 4 + nMaxAdditionalMsgBuffersForProtocols, 
				nMessageSize)) {
			return false;
		}

		if(!pool.create(RecyclableBuffer.Type.Double, 
				nMaxPoolConnections, nMessageSize * 2)) {
			return false;
		}

		if(!pool.create(RecyclableBuffer.Type.Multiple, 
				nMaxPoolConnections, 
				options.getMaxPendingOutgoingMessages() * nMessageSize)) {
			return false;
		}
		
		if(!pool.create(RecyclableBuffer.Type.Socket, 
				nMaxPoolConnections * 2, options.getSocketBufferSize())) {
			return false;
		}
		
		return true;
	}
	
	private boolean initConnectionPool(int nMaxPoolConnections) {
		if (!thePhysicalConnectionPool.initialize(nMaxPoolConnections)) {
			return false;
		}

		if (!theLogicalConnectionPool.initialize(nMaxPoolConnections)) {
			return false;
		}
		if (options.challengeClients() && 
				!theConnectionContextPool.initialize(nMaxPoolConnections)) {
				return false;
		}
		Debug.debug("Connection Context Created");

		if(!initializeProtocolContextPools(nMaxPoolConnections)) {
			return false;
		}

		Debug.debug("Protocol Context Created");
		
		return true;
	}
	
	private void initComponent() {
		pool = BufferPool.getDefaultPool();

		options = new ServerOptions();
		
		thePhysicalConnectionPool = new PhysicalConnectionPool(this);
		theLogicalConnectionPool = new LogicalConnectionPool(this);
		theConnectionContextPool = new ConnectionContextPool(this);

		theProtocolManager = new ProtocolManager();
		broadcastManager = new BroadcastManager(this);
		dispatcher = new Dispatcher(this);

		stats = new ServerStats(options, dispatcher.getServiceNames(), 
				broadcastManager.getQueuesNames());

		ioQueue = new IOQueueImpl();
		clientFactoryImpl = new ClientFactory(this);
		demux = new Demultiplexor(this);
		garbageCollector = new GarbageCollector(theLogicalConnectionPool);
		
		// Monitor
		monitorAcceptor = new MonitorAcceptor(this);
		monitorBroadcastManager = new MonitorsBroadcastManager(this);
	}
	
	public MonitorAcceptor getMonitorAcceptor() { return monitorAcceptor; }
	
	public MonitorsBroadcastManager getMonitorsBroadcastManager() {
		return monitorBroadcastManager;
	}
	
	public ClientFactory getClientFactory() { return clientFactoryImpl; }
	
	public Demultiplexor getDemultiplexor() { return demux; }
	
	public GarbageCollector getGarbageCollector() { return garbageCollector; }
	
	public BroadcastManager getBroadcastManager() { return broadcastManager; }
	
	public Dispatcher getDispatcher() { return dispatcher; }
	
	public IOQueue<PhysicalConnection> getIOQueue() { return ioQueue; }
	
	public ServerStats getServerStats() { return stats; }

	public ServerOptions getServerOptions() { return options; }
	
	public ProtocolManager getProtocolManager() {
		return theProtocolManager;
	}
	
	public PhysicalConnectionPool getPhysicalConnectionPool() {
		return thePhysicalConnectionPool;
	}
	
	public LogicalConnectionPool getLogicalConnectionPool() {
		return theLogicalConnectionPool;
	}
	
	public ConnectionContextPool getConnectionContextPool() {
		return theConnectionContextPool;
	}

	public boolean createListener(int port, ListenerOptions pOptions) {
		Integer key = new Integer(port);
		if (listenersMap.containsKey(key)) {
			return false;
		}

		Acceptor acceptor = new Acceptor(this);
		acceptor.setListeningPort(port);
		acceptor.setOptions(pOptions);

		listenersMap.put(key, acceptor);
	    return true;
	}

	public void registerService(Service service) {
	    dispatcher.registerService(service);
	}

	public String getServerInfos() {
	    return this.serverInfos;
	}

	public void setServerInfos(String serverInfos) {
	    this.serverInfos = serverInfos;
	}

	public boolean start(boolean startInSeparateThread) {
		int nMaxPoolConnections = options.getMaxConnections() + 10;
		
		if (!initializeBufferPool(nMaxPoolConnections)) {
			return false;
		}
		Debug.debug("Buffer Pool Created");
		
		/* Initialize the pool of connection */
		if (!initConnectionPool(nMaxPoolConnections)) {
			return false;
		}
		Debug.debug("Connection Pool Created");

		/* Create the I/O Queue to monitor the I/O event */
	    if (!ioQueue.create()) {
	        return false;
	    }
		Debug.debug("IO Queue Created");

		/* Start the multiplexer to process the I/O event  */
	    if (!demux.start()) {
	        ioQueue.free();
	        return false;
	    }
		Debug.debug("Demux Created");

		/* Start the listener to listen request from client */
	    if (!startListening()) {
	        demux.stop();
	        ioQueue.free();
			return false;
	    }
		Debug.debug("Listener Started");

		/* Start the listener to monitor service if required */
	    if (options.isMonitorEnabled()) {
			monitorAcceptor.setListeningPort(options.getMonitorPort());
			if (!monitorAcceptor.startListening()) {
				stopListening();
				demux.stop();
				ioQueue.free();
				return false;
			}

			monitorsStreamer = new BroadcastStreamer();
			monitorsStreamer.start();
	    }
		Debug.debug("Monitor Listener Started");

		/* Start the broadcast streamer */
	    startStreamers();
		Debug.debug("Streamers Started");
	    
	    isRunning = true;

	    /* Start loop to do periodic work */
		Debug.debug("Start to accept client");
	    if (startInSeparateThread) {
	    	hThread = new Thread(new Runnable() {

				public void run() {
					eternalLoop();
				}
	    		
	    	});
	    	hThread.start();
	    } else {
	        eternalLoop();
	    }

		return true;
	}

	public void stop() {
		isRunning = false;
		if (hThread != null) {
			LockSupport.unpark(hThread);

			try {
				hThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	    if (options.isMonitorEnabled()) {
	        monitorAcceptor.stopListening();
			monitorsStreamer.stop();
			monitorsStreamer = null;
		}

	    stopListening();

	    demux.stop();

	    clientFactoryImpl.stop();

		stopStreamers();

	    broadcastManager.disposeAllPackets();
		monitorBroadcastManager.disposeAllPackets();

	    ioQueue.free();

	    garbageCollector.activate(true);
	}

	public void pause(){
		stopListening();
	}

	public void resume(){
		startListening();
	}

	public void setMessageFactory(MessageFactory pMsgFactory) {
		this.msgFactory = pMsgFactory;
	}

	public MessageFactory getMessageFactory() { return msgFactory; }

	public void startStreamers() {
		BroadcastStreamer streamer;
		for (int i = 0; i < options.getStreamers(); i++) {
			streamer = new BroadcastStreamer();
			streamersList.add(streamer);
			streamer.start();
		}
	}

	public void stopStreamers() {
		Iterator<BroadcastStreamer> it = streamersList.iterator();
		BroadcastStreamer streamer;
		while (it.hasNext()) {
			streamer = it.next();
			streamer.stop();
			it.remove();
		}
	}

	public void reshuffleStreamers() {
		Iterator<BroadcastStreamer> it = streamersList.iterator();
		BroadcastStreamer streamer;
		while (it.hasNext()) {
			streamer = it.next();
			streamer.awakeAll();
		}
	}
	
	public void reshuffleMonitorsStreamer() {
		monitorsStreamer.awakeAll();
	}

	public void addClientToStreamers(
			LogicalConnectionImpl logicalConnection) {
		BroadcastManagerBase broadcastManagerRef = logicalConnection.isMonitor() 
			?  monitorBroadcastManager : broadcastManager;
		List<String> globalQueues = broadcastManagerRef.getGlobalQueues();

		for (String queue : globalQueues) {
			logicalConnection.subscribeToQueue(queue);
		}

		if (logicalConnection.isMonitor()) {
			monitorsStreamer.addItem(logicalConnection);
			return;
		}

		BroadcastStreamer lessBusy = null;

		for (BroadcastStreamer streamer : streamersList) {
			if (lessBusy != null) {
				if(lessBusy.getItemsCount() > streamer.getItemsCount()) {
					lessBusy = streamer;
				}
			} else {
				lessBusy = streamer;
			}
		}

		if (lessBusy != null) {
			lessBusy.addItem(logicalConnection);
			logicalConnection.setStreamer(lessBusy);
		}
	}

	public void createQueue(String queueName, QueueOptions queueOptions) {
		broadcastManager.createBroadcastQueue(queueName, queueOptions);
	}

	public void removeQueue(String queueName) {
		broadcastManager.removeBroadcastQueue(queueName);
	}

	public boolean pushPacket(OutgoingPacket pPacket, String queueName) {
		return broadcastManager.pushPacket(pPacket, queueName, "", 0);
	}

	public boolean pushPacket(OutgoingPacket pPacket, String queueName, 
			String killKey, int objectCategory) {
		return broadcastManager.pushPacket(pPacket, queueName, killKey, 
				objectCategory);
	}

	public void removePacketFromQueue(String killKey, int objectCategory, 
			String queueName) {
		broadcastManager.removePacket(killKey, objectCategory, queueName);
	}

	public Server getFacade() { return facade; }
}
