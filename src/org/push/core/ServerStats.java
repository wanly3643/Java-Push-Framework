package org.push.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.push.impl.xml.XMLPacket;
import org.push.measures.CumulMeasure;
import org.push.measures.CumulMeasureArgs;
import org.push.measures.DistributionMeasure;
import org.push.measures.DistributionMeasureArgs;
import org.push.measures.DurationMeasure;
import org.push.measures.DurationMeasureArgs;
import org.push.measures.KeyedAveragedDistributionMeasure;
import org.push.measures.KeyedAveragedDistributionMeasureArgs;
import org.push.measures.KeyedAveragedMeasure;
import org.push.measures.MKAveragedMeasureArgs;
import org.push.measures.Measure;
import org.push.monitor.AnalyticsProtocol;
import org.push.protocol.OutgoingPacket;
import org.push.util.CppEnum;
import org.push.util.Utils;

/**
 * To manage the state information to the server.
 * 
 * @author Lei Wang
 */

public class ServerStats {

	public static enum Measures  implements CppEnum  {
        VisitorsOnline(1), // Inc at Reactor::ProcessFirstPacket (when client is new) Dec at ClientFactoryImpl::disposeClient
        VisitorsHitsIn(2),//Inc at Reactor::ProcessFirstPacket (whether client is new or not)
        VisitorsHitsOut(3), // ClientFactoryImpl::disposeClient
        VisitorsSYNs(4),	// inc at CAcceptor::handleAcceptedSocket
        VisitorsDuration(5), //inc duration at CClientFactoryImpl::disposeClient
        VisitorsBounce(6),
        BandwidthInbound(7),//CReactor::OnReceiveComplete
        BandwidthOutbound(8),//CReactor::OnWriteComplete
        BandwidthRejection(9),//CChannel::SendData
        BandwidthOutstanding(10),//CChannel::SendData
        BandwidthInboundVolPerRequest(11), //CReactor::dispatchRequest
        BandwidthOutboundVolPerRequest(12),	//CChannel::SendData
        BandwidthInboundPerConnection(13), //CReactor::OnReceiveComplete
        BandwidthOutboundPerConnection(14), //CReactor::OnWriteComplete
        PerformanceRequestVolPerRequest(15), //CReactor::dispatchRequest
        PerformanceProcessingTime(16),	//CReactor::dispatchRequest
        PerformanceProcessingTimePerService(17),	//CReactor::dispatchRequest
        QoSFillRatePerChannel(18),
        QoSSendRatePerChannel(19),
        QoSAvgSendRatePerChannel(20);

        private int value;

        private Measures(int value) { this.value = value; }

        public int value() { return this.value; }

    };

	private ServerOptions options;

	private String serviceNames;

	private String queuesNames;

	// Where the different measures are stored
	private ConcurrentMap<Measures, Measure> measuresMap = 
		new ConcurrentHashMap<Measures, Measure>();

	// How many packets about performance have been created
	private AtomicInteger packetCounter;

	public ServerStats(ServerOptions options, String serviceNames, 
			String queuesNames) {
		this.options = options;
		this.serviceNames = serviceNames;
		this.queuesNames = queuesNames;
	    packetCounter = new AtomicInteger(0);
	    init();
	}

	/**
	 * Initialization, create all the types of measures.
	 * Later, each measure will be added into by the type
	 */
	public void init() {
	    //Visitors
	    measuresMap.put(Measures.VisitorsOnline, new CumulMeasure("online", true));
	    measuresMap.put(Measures.VisitorsHitsIn, new CumulMeasure("hitsIn"));
	    measuresMap.put(Measures.VisitorsHitsOut, new CumulMeasure("hitsOut"));
	    measuresMap.put(Measures.VisitorsSYNs, new CumulMeasure("syn"));
	    measuresMap.put(Measures.VisitorsDuration, new DurationMeasure("vstDur"));
	    measuresMap.put(Measures.VisitorsBounce, new CumulMeasure("bounce"));

	    //Bandwidth
	    measuresMap.put(Measures.BandwidthInbound, new CumulMeasure("inbound"));
	    measuresMap.put(Measures.BandwidthOutbound, new CumulMeasure("outbound"));
	    measuresMap.put(Measures.BandwidthRejection, new CumulMeasure("rejection"));
	    measuresMap.put(Measures.BandwidthOutstanding, new CumulMeasure("outstanding"));
	    measuresMap.put(Measures.BandwidthInboundVolPerRequest, new DistributionMeasure("inbound_by_service"));
	    measuresMap.put(Measures.BandwidthOutboundVolPerRequest, new DistributionMeasure("outbound_by_service"));
	    measuresMap.put(Measures.BandwidthInboundPerConnection, new KeyedAveragedMeasure("inboundc"));
	    measuresMap.put(Measures.BandwidthOutboundPerConnection, new KeyedAveragedMeasure("outboundc"));

	    //Performance
	    measuresMap.put(Measures.PerformanceRequestVolPerRequest, new DistributionMeasure("request_by_service"));
	    measuresMap.put(Measures.PerformanceProcessingTime, new DurationMeasure("processing"));
	    measuresMap.put(Measures.PerformanceProcessingTimePerService, new DistributionMeasure("processing_by_service"));

	    //Qos Broadcast
	    measuresMap.put(Measures.QoSFillRatePerChannel, new DistributionMeasure("fillrate_by_queue"));
	    measuresMap.put(Measures.QoSSendRatePerChannel, new DistributionMeasure("sendrate_by_queue"));
	    measuresMap.put(Measures.QoSAvgSendRatePerChannel, new KeyedAveragedDistributionMeasure("avgsendrate_by_queue"));

	}

	/**
	 * Add a <code>CumulMeasureArgs</code> into the measure
	 * 
	 * @param measureId  the id of the measure type
	 * @param value  parameter for <code>CumulMeasureArgs</code>
	 */
	public void addToCumul(Measures measureId, double value) {
	    if (!options.isProfilingEnabled())
	        return;

	    Measure m = measuresMap.get(measureId);
	    if (m != null) {
	    	synchronized (m) {
			    CumulMeasureArgs args = new CumulMeasureArgs();
			    args.value = value;
		    	m.addObservation(args);
	    	}
	    }
	}


	/**
	 * Add a <code>DurationMeasureArgs</code> into the measure
	 * 
	 * @param measureId  the id of the measure type
	 * @param value  parameter for <code>DurationMeasureArgs</code>
	 */
	public void addToDuration(Measures measureId, double value) {
	    if (!options.isProfilingEnabled())
	        return;

	    Measure m = measuresMap.get(measureId);
	    if (m != null) {
	    	synchronized (m) {
		    	DurationMeasureArgs args = new DurationMeasureArgs();
		    	args.duration = value;
		    	m.addObservation(args);
	    	}
	    }
	}

	/**
	 * Add a <code>DistributionMeasureArgs</code> into the measure
	 * 
	 * @param measureId  the id of the measure type
	 * @param serviceName  parameter for <code>DistributionMeasureArgs</code>
	 * @param value  parameter for <code>DistributionMeasureArgs</code>
	 */
	public void addToDistribution(Measures measureId, String serviceName, double value) {
	    if (!options.isProfilingEnabled())
	        return;

	    Measure m = measuresMap.get(measureId);
	    if (m != null) {
	    	synchronized (m) {
			    DistributionMeasureArgs args = new DistributionMeasureArgs();
			    args.serviceName = serviceName;
			    args.value = value;
		    	m.addObservation(args);
	    	}
	    }
	}

	/**
	 * Add a <code>MKAveragedMeasureArgs</code> into the measure
	 *
	 * @param measureId  the id of the measure type
	 * @param key  parameter for <code>MKAveragedMeasureArgs</code>
	 * @param value  parameter for <code>MKAveragedMeasureArgs</code>
	 */
	public void addToKeyedDuration(Measures measureId, int key, double value)
	{
	    if (!options.isProfilingEnabled())
	        return;
	    
	    Measure m = measuresMap.get(measureId);
	    if (m != null) {
	    	synchronized (m) {
			    MKAveragedMeasureArgs args = new MKAveragedMeasureArgs();
			    args.value = value;
			    args.key = key;
		    	m.addObservation(args);
	    	}
	    }
	}

	/**
	 * Add a <code>KeyedAveragedDistributionMeasureArgs</code> into the measure
	 * 
	 * @param measureId  the id of the measure type
	 * @param segmentName  parameter for <code>KeyedAveragedDistributionMeasureArgs</code>
	 * @param key  parameter for <code>KeyedAveragedDistributionMeasureArgs</code>
	 * @param value  parameter for <code>KeyedAveragedDistributionMeasureArgs</code>
	 */
	public void addToKeyedDistributionDuration(Measures measureId, 
			String segmentName, String key, double value) {
	    if (!options.isProfilingEnabled())
	        return;
	    
	    Measure m = measuresMap.get(measureId);
	    if (m != null) {
	    	synchronized (m) {
		    	KeyedAveragedDistributionMeasureArgs args = new KeyedAveragedDistributionMeasureArgs();
			    args.key = key;
			    args.serviceName = segmentName;
			    args.value = value;
		    	m.addObservation(args);
	    	}
	    }
	}

	/**
	 * Build a packet containing the full information of 
	 * performance, this packet will be sent to the monitor
	 * 
	 * @return  An outgoing packet 
	 */
	public OutgoingPacket getPerformancePacket() {
	    String timestamp  = Utils.getCurrentTime();
	    
	    StringBuilder sb = new StringBuilder();
	    
	    //Write header :
	    sb.append("<root typeId=\"");
	    sb.append(AnalyticsProtocol.MeasuresResponse.value());
	    sb.append("\">");
	    sb.append("<stats>");
	    sb.append("<id val=\"").append(packetCounter.incrementAndGet()).append( "\"/>");
	    sb.append("<timestamp val=\"").append(timestamp).append("\"/>");

	    //Visitors :
	    sb.append(measuresMap.get(Measures.VisitorsOnline).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.VisitorsHitsIn).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.VisitorsHitsOut).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.VisitorsSYNs).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.VisitorsDuration).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.VisitorsBounce).collectAndReset(timestamp));

	    //Bandwidth :
	    sb.append(measuresMap.get(Measures.BandwidthInbound).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthOutbound).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthRejection).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthOutstanding).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthInboundVolPerRequest).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthOutboundVolPerRequest).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthInboundPerConnection).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.BandwidthOutboundPerConnection).collectAndReset(timestamp));

	    //Performance :
	    sb.append(measuresMap.get(Measures.PerformanceRequestVolPerRequest).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.PerformanceProcessingTime).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.PerformanceProcessingTimePerService).collectAndReset(timestamp));


	    //QoS Broadcast :
	    sb.append(measuresMap.get(Measures.QoSFillRatePerChannel).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.QoSSendRatePerChannel).collectAndReset(timestamp));
	    sb.append(measuresMap.get(Measures.QoSAvgSendRatePerChannel).collectAndReset(timestamp));

	    sb.append("</stats>");
	    sb.append("</root>");

	    return new XMLPacket(sb.toString());
	}


	/**
	 * Build a packet containing the full information of 
	 * setting of the server, this packet will be sent 
	 * to the monitor
	 * 
	 * @return  An outgoing packet 
	 */
	public OutgoingPacket getInitializationPacket() {
		StringBuilder sb = new StringBuilder();

	    //Write header :
	    sb.append("<root typeId=\"");
	    sb.append(AnalyticsProtocol.InitializationResponse.value());
	    sb.append("\">");
	    sb.append("<init>");

	    sb.append("<sampling val=\"").append(options.getSamplingRate()).append("\"/>");
	    sb.append("<services>");
	    sb.append(serviceNames);

	    sb.append("</services>");

	    sb.append("<queues>");
	    sb.append(queuesNames);
	    sb.append("</queues>");


	    sb.append("</init>");
	    sb.append("</root>");

	    return new XMLPacket(sb.toString());
	}
}
