package org.push.monitor;

import org.push.impl.xml.XMLPacket;
import org.push.protocol.Buffer;
import org.push.protocol.DeserializeData;
import org.push.protocol.IncomingPacket;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.ErrorCodes.DeserializeResult;
import org.push.protocol.ErrorCodes.SerializeResult;

/**
 * 
 * @author Lei Wang
 */

public class MonitorsMsgFactory extends MessageFactory {

	private static MonitorsMsgFactory sigleton = new MonitorsMsgFactory();
	
	public static MonitorsMsgFactory getDefault() {
		return sigleton;
	}

	private MonitorsMsgFactory() {}

	@Override
	public SerializeResult serializeMessage(OutgoingPacket outgoingPacket,
			Buffer buffer) {
		XMLPacket response = (XMLPacket) outgoingPacket;
		if (!response.encode()) {
			return SerializeResult.Failure;
		}

		if (!buffer.append(response.getData().getBytes())) {
			return SerializeResult.InsufficientBufferSpace;
		}

		return SerializeResult.Success;
	}

	@Override
	public DeserializeResult deserializeMessage(Buffer contentBytes,
			DeserializeData deserializeData) {
		XMLPacket request = new XMLPacket();
		if (!request.decode(new String(contentBytes.getBuffer()))) {
			return DeserializeResult.Failure;
		}

		//TODO pMessage = dynamic_cast<IncomingPacket*> (pRequest);
		deserializeData.setMessage(request);
		
		deserializeData.setRoutingService(request.getTypeId().value());
		return DeserializeResult.Success;
	}

	@Override
	public void disposeIncomingPacket(IncomingPacket packet) {
		// Nothing to do
	}

	@Override
	public void disposeOutgoingPacket(OutgoingPacket packet) {
		// Nothing to do
	}
	
	public boolean preSerializeMessage(OutgoingPacket outgoingPacket) {
		XMLPacket response = (XMLPacket) outgoingPacket;
		return response.encode();
	}

}
