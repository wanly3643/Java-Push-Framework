package org.push.impl.http;

import org.push.protocol.Buffer;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolContext;
import org.push.protocol.ErrorCodes.DecodeResult;
import org.push.protocol.ErrorCodes.EncodeResult;

public class HttpProtocol extends Protocol {

	public HttpProtocol() {
	}

	@Override
	public void startSession(ProtocolContext context, Buffer outgoingBytes) {

	}

	@Override
	public boolean readData(ProtocolContext context, Buffer incomingBytes) {
		return false;
	}

	@Override
	public DecodeResult tryDecode(ProtocolContext context, 
			Buffer outputBuffer) {
		return null;
	}

	@Override
	public EncodeResult encodeContent(ProtocolContext context,
			Buffer inputBuffer, Buffer outputBuffer) {
		return null;
	}

}
