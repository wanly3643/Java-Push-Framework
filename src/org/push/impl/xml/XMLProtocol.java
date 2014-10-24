package org.push.impl.xml;

import org.push.protocol.Buffer;
import org.push.protocol.ErrorCodes.DecodeResult;
import org.push.protocol.ErrorCodes.EncodeResult;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolContext;

/**
 * The protocol use the XML as the data format.
 * 
 * @author Lei Wang
 */

public class XMLProtocol extends Protocol {

	public XMLProtocol() { }
	
	public ProtocolContext createNewProtocolContext() {
		return new ProtocolContext();
	}

	@Override
	public void startSession(ProtocolContext context, Buffer outgoingBytes) {
		context.setInitialized();
	}

	@Override
	public boolean readData(ProtocolContext context, Buffer incomingBytes) {
		return context.getDataBuffer().append(incomingBytes);
	}

	@Override
	public DecodeResult tryDecode(ProtocolContext context, Buffer outputBuffer) {
		Buffer inputBuffer = context.getDataBuffer();

		return outputBuffer.append(inputBuffer.getBuffer(), 
				inputBuffer.getDataSize()) ? 
				DecodeResult.Content : DecodeResult.Failure;
	}

	@Override
	public EncodeResult encodeContent(ProtocolContext context,
			Buffer inputBuffer, Buffer outputBuffer) {
		//Just append an end character:
		int requiredBytes = inputBuffer.getDataSize() + 1;
		if (requiredBytes < outputBuffer.getRemainingSize())
		{
			return EncodeResult.InsufficientBufferSpace;
		}
		outputBuffer.append(inputBuffer);

		return EncodeResult.Success;
	}
}
