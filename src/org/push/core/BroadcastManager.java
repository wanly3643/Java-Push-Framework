package org.push.core;

import org.push.protocol.OutgoingPacket;
import org.push.util.Utils;

/**
 * To manage to broadcast message to the common client.
 * 
 * @author Lei Wang
 */

public class BroadcastManager extends BroadcastManagerBase {

	private ServerImpl serverImpl;
	
	public BroadcastManager(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	}

	protected void activateSubscribers(String channelName) {
		serverImpl.reshuffleStreamers();
	}

	protected void deleteOutgoingPacket(OutgoingPacket packet) {
		serverImpl.getMessageFactory().disposeOutgoingPacket(packet);
	}

	protected void preEncodeOutgoingPacket(OutgoingPacket packet) {
		serverImpl.getMessageFactory().preSerializeMessage(packet);
	}

	protected void handleOnBeforePushPacket(String channelName) {
		ServerStats stats = serverImpl.getServerStats();
		stats.addToDistribution(ServerStats.Measures.QoSFillRatePerChannel, 
				channelName, 1);
	}

	protected void handleOnAfterPacketIsSent(String channelName, 
			String subscriberKey) {
		ServerStats stats = serverImpl.getServerStats();
		stats.addToDistribution(ServerStats.Measures.QoSSendRatePerChannel,
				channelName, 1);
		stats.addToKeyedDistributionDuration(
				ServerStats.Measures.QoSAvgSendRatePerChannel, channelName, 
				subscriberKey, 1);
	}
}
