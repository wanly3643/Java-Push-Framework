package org.push.impl.xml;

import org.push.protocol.Buffer;
import org.push.protocol.DeserializeData;
import org.push.protocol.IncomingPacket;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.ErrorCodes.DeserializeResult;
import org.push.protocol.ErrorCodes.SerializeResult;
import org.push.util.Utils;

/**
 * To serialize the XML packet into binary data and de-serialize
 * the binary data into XML packet.
 * 
 * @author Lei Wang
 */

public class XMLSerializer extends MessageFactory {

	@Override
	public SerializeResult serializeMessage(OutgoingPacket outgoingPacket,
			Buffer buffer) {
		XMLPacket message = (XMLPacket)outgoingPacket;

		message.encode();
		
		String str = message.getData();
		byte[] data = Utils.stringToBytes(str);
		
		if (data == null) {
			return SerializeResult.Failure;
		}

		if (buffer.getCapacity() < (data.length + 1))
		{
			return SerializeResult.InsufficientBufferSpace; //TODO. Verify this.
		}

		buffer.append(data, data.length);
		
		return SerializeResult.Success;
	}

	@Override
	public DeserializeResult deserializeMessage(Buffer contentBytes,
			DeserializeData deserializeData) {
		if(contentBytes.getDataSize() == 0) {
			return DeserializeResult.DiscardContent;
		}
		
		String str = Utils.bytesToString(contentBytes.getBuffer(), 
				contentBytes.getDataSize());
		
		if (str == null) {
			return DeserializeResult.Failure;
		}
		
		System.out.println("XML content:\n" + str);
	 
		XMLPacket packet = new XMLPacket();

		if (!packet.decode(str)) {
			return DeserializeResult.Failure;
		}
		deserializeData.setMessage(packet);
		deserializeData.setRoutingService(packet.getTypeId().value());

		return DeserializeResult.Success;
	}

	@Override
	public void disposeIncomingPacket(IncomingPacket packet) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disposeOutgoingPacket(OutgoingPacket packet) {
		// TODO Auto-generated method stub

	}

}
