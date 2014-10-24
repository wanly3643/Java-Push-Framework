package org.push.core;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.push.core.Common.DisconnectionReason;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.core.Common.SendResult;
import org.push.protocol.OutgoingPacket;
import org.push.util.Debug;
import org.push.util.Utils;

/**
 * Connection associated with Protocol.
 * 
 * @author Lei Wang
 */

public class LogicalConnectionImpl {

	private LogicalConnection facade;
	private PhysicalConnection physicalConnection;
	private long dtConnectTime;
	private AtomicLong nRefrenceCounter = new AtomicLong(0);
	private BroadcastStreamer streamer;
	private BroadcastQueueGroupContext rootQueueGroupContext;
	private boolean skipCheckNextPacket;
	private ReadWriteLock subscriptionDataLock = new ReentrantReadWriteLock();
	private AtomicLong broadcastRunFlag = new AtomicLong(0);
	
	private ServerImpl serverImpl;

	public LogicalConnectionImpl(LogicalConnection facade, 
			ServerImpl serverImpl) {
		this.facade = facade;
		this.rootQueueGroupContext = null;
		this.physicalConnection = null;
		this.serverImpl = serverImpl;
	}
	
	public BroadcastStreamer getStreamer() { return this.streamer; }

	public void setStreamer(BroadcastStreamer streamer) {
		this.streamer = streamer;
	}
	
	public PhysicalConnection getPhysicalConnection() {
		return physicalConnection;
	}

	public void recycle() {
		if (rootQueueGroupContext != null) {
			rootQueueGroupContext = null;
		}

		if (facade != null) {
			facade.recycle();
		}

		if (physicalConnection != null) {		
			PhysicalConnectionPool thePhysicalConnectionPool = 
				serverImpl.getPhysicalConnectionPool();

			thePhysicalConnectionPool.returnObject(physicalConnection);
			physicalConnection	= null;
		}
	}

	public void incrementUsage() {
		nRefrenceCounter.incrementAndGet();
	}

	public void decrementUsage() {
		nRefrenceCounter.decrementAndGet();
	}

	public boolean canDelete() {
		return nRefrenceCounter.longValue() == 0 && 
			physicalConnection.checkIfUnusedByIOWorkers();
	}

	public boolean isMonitor() { return false; }

	public boolean checkAndProcessPendingBroadcast(boolean somethingHasHappened) {
		subscriptionDataLock.readLock().lock();

		boolean ret = true;

		if (somethingHasHappened) {
			skipCheckNextPacket = false;
		}

		if (skipCheckNextPacket) {
			subscriptionDataLock.readLock().unlock();
			return ret;
		}

		// Covers the case when connection is not subscribed to any queue.
		if (rootQueueGroupContext == null) {
			subscriptionDataLock.readLock().unlock();
			return ret;
		}

		if (physicalConnection.isWriteInProgress()) {
			subscriptionDataLock.readLock().unlock();
			// We know that this method will be called again shortly.
			return ret;
		}

		if (!broadcastRunFlag.compareAndSet(0, 1)) {
			subscriptionDataLock.readLock().unlock();
			 //Someone else is running here, we do not want 
			// to send duplicate messages.
			return ret;
		}
		boolean isPacketFound = false;

		BroadcastQueueGroupContext group = rootQueueGroupContext;
		PacketInfo packetInfo;
		OutgoingPacket outgoingPacket;
		while (group != null) {
			packetInfo = group.getNextPacket();
			if (packetInfo == null) {
				group = group.next();
				continue;
			}

			isPacketFound = true;
			outgoingPacket = packetInfo.getPacket();

			SendResult result = pushPacket(outgoingPacket);

			boolean isSent = (result == SendResult.OK);

			group.returnPacket(packetInfo, isSent);

			 //(ret == SendResult::Retry || ret == SendResult::NotOK)
			if (!isSent) {
				if(result == SendResult.NotOK) {
					ret = false;
				}
				break;
			}
		}

		if (!isPacketFound) {
			skipCheckNextPacket = true;
		}

		broadcastRunFlag.set(0);
		subscriptionDataLock.readLock().unlock();
		return ret;
	}

	public void initializeIntern(PhysicalConnection physicalConnection) {
		this.physicalConnection = physicalConnection;
		this.physicalConnection.attachToClient(this);

		this.dtConnectTime = System.currentTimeMillis();
		this.nRefrenceCounter = new AtomicLong(0);
		this.streamer = null;
		this.rootQueueGroupContext = null;
		this.skipCheckNextPacket = false;
		this.broadcastRunFlag = new AtomicLong(0);
	}

	public LogicalConnection getFacade() { return this.facade; }

	public SendResult pushPacket(OutgoingPacket packet) {
		SendResult result = physicalConnection.pushPacket(packet);
		if (result == SendResult.NotOK) {
			serverImpl.getClientFactory().disconnect(this, false, 
					DisconnectionReason.PeerClosure);
		}
		return result;
	}

	public SendResult tryPushPacket(OutgoingPacket pPacket) {
		SendResult result = physicalConnection.tryPushPacket(pPacket);
		if (result == SendResult.NotOK) {
			serverImpl.getClientFactory().disconnect(this, false, 
					DisconnectionReason.PeerClosure);
		}
		return result;
	}

	public void disconnect(boolean waitForPendingPackets) {
		serverImpl.getClientFactory().disconnect(this, waitForPendingPackets, 
				DisconnectionReason.RequestedClosure);
	}

	public double getVisitDuration() {
		return Utils.timePassed(dtConnectTime);
	}

	public boolean isInactive() {
		double uInactivityTime = physicalConnection.getTimeToLastReceive();

		if (uInactivityTime > 
				serverImpl.getServerOptions().getMaxClientIdleTime()) {
			Debug.debug("Expired: " + uInactivityTime);
			return true;
		}

		return false;
	}

	public boolean subscribeToQueue(String queueName) {
		return subscribeToQueue(queueName, false);
	}

	public boolean subscribeToQueue(String queueName, 
			boolean ignorePreviousPackets) {
		subscriptionDataLock.writeLock().lock();

		// Locate the queue.
		BroadcastQueue queue = serverImpl.getBroadcastManager()
			.getQueue(queueName);
		if (queue == null) {
			subscriptionDataLock.writeLock().unlock();
			return false;
		}

		// Create a queue context to hold our last sent packet Id etc.
		BroadcastQueueContext info = new BroadcastQueueContext(queue, 
				ignorePreviousPackets ? queue.getLastGeneratedId() : 0);

		// Insert this context in the ordered chain of all contexts 
		// (based on queue priority and quota attributes):
		if (rootQueueGroupContext == null || 
				!rootQueueGroupContext.insert(info)) {
			BroadcastQueueGroupContext group = 
				new BroadcastQueueGroupContext(info);
			group.setNext(rootQueueGroupContext);
			rootQueueGroupContext = group;
		}

		subscriptionDataLock.writeLock().unlock();

		return true;
	}

	public void unSubscribeFromQueue(String queueName) {
		subscriptionDataLock.writeLock().lock();

		if (rootQueueGroupContext != null) {
			rootQueueGroupContext.remove(queueName);
			if (rootQueueGroupContext.root() == null) {
				BroadcastQueueGroupContext temp = rootQueueGroupContext;
				rootQueueGroupContext = temp.next();
				temp.setNext(null);
			}
		}
		
		subscriptionDataLock.writeLock().unlock();
	}

	public void unSubscribeFromAll() {
		subscriptionDataLock.writeLock().lock();

		if (rootQueueGroupContext != null) {
			rootQueueGroupContext = null;
		}

		subscriptionDataLock.writeLock().unlock();

	}

	public void onReadyForSend(int nAvailableSpace) {
		Utils.unsignedIntArgCheck(nAvailableSpace, "nAvailableSpace");
		facade.onReadyForSend(nAvailableSpace);
	}

	public void onDisconnected(DisconnectionReason closeReason) {
		facade.onDisconnected(closeReason);
	}

	public void onConnected() {
		facade.onConnected();
	}

	public boolean onBeforeIdleClose() {
		return facade.onBeforeIdleClose();
	}

	public Login processLogin(LoginData loginData) {
		return facade.processLogin(loginData);
	}
}
