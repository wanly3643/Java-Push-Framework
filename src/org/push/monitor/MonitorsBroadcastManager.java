package org.push.monitor;

import org.push.core.BroadcastManagerBase;
import org.push.core.QueueOptions;
import org.push.core.ServerImpl;
import org.push.protocol.OutgoingPacket;
import org.push.util.Utils;

/**
 * 
 * @author Lei Wang
 */

public class MonitorsBroadcastManager extends BroadcastManagerBase {

	//
	private ServerImpl serverImpl;

	public MonitorsBroadcastManager(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;

	    QueueOptions options = new QueueOptions();
	    //
	    options.setMaxPackets(100);
	    options.setRequireRegistration(true);
	    options.setPriority(10);
	    options.setPacketsQuota(10);
	    options.setFillRateThrottlingPeriod(60);
	    options.setFillRateThrottlingMaxPackets(100);

	    createBroadcastQueue("stats", options);

	    options = new QueueOptions();
	    //
	    options.setMaxPackets(1000);
	    options.setRequireRegistration(true);
	    options.setPriority(5);
	    options.setPacketsQuota(5);
	    options.setFillRateThrottlingPeriod(60);
	    options.setFillRateThrottlingMaxPackets(100);

	    createBroadcastQueue("clientsIn", options);

	    options = new QueueOptions();
	    //
	    options.setMaxPackets(50);
	    options.setRequireRegistration(true);
	    options.setPriority(10);
	    options.setPacketsQuota(10);
	    options.setFillRateThrottlingPeriod(60);
	    options.setFillRateThrottlingMaxPackets(100);

	    createBroadcastQueue("clientsOut", options);
	}

	@Override
	protected void preEncodeOutgoingPacket(OutgoingPacket packet) {
		//Nothing to do, packet is already encoded.
	}

	@Override
	protected void deleteOutgoingPacket(OutgoingPacket packet) {
		//Nothing to do
	}

	@Override
	protected void activateSubscribers(String channelName) {
		serverImpl.reshuffleMonitorsStreamer();
	}
}
