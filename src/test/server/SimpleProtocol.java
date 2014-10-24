package test.server;

import org.push.protocol.Buffer;
import org.push.protocol.Protocol;
import org.push.protocol.ProtocolContext;
import org.push.protocol.ErrorCodes.DecodeResult;
import org.push.protocol.ErrorCodes.EncodeResult;
import org.push.util.Utils;

public class SimpleProtocol extends Protocol {

	public static final int SignatureStart = 0;
	public static final int SignatureEnd = 0;

	@Override
	public void startSession(ProtocolContext context, Buffer outgoingBytes) {
		outgoingBytes.clearBytes();
		context.setInitialized();
	}

	@Override
	public boolean readData(ProtocolContext context, Buffer incomingBytes) {
		return context.getDataBuffer().append(incomingBytes);
	}

	@Override
	public DecodeResult tryDecode(ProtocolContext context, Buffer outputBuffer) {

		Buffer inputbuffer = context.getDataBuffer();

		if (inputbuffer.getDataSize() < 6)
			return DecodeResult.WantMoreData;

		byte[] bytesBuffer = new byte[2];
		bytesBuffer[0] = inputbuffer.getAt(0);
		bytesBuffer[1] = inputbuffer.getAt(1);
		
		int sStart = Utils.bytesToInt(bytesBuffer);

		if (sStart != SignatureStart)
			return DecodeResult.Failure;

		outputBuffer.clearBytes();
		bytesBuffer[0] = inputbuffer.getAt(2);
		bytesBuffer[1] = inputbuffer.getAt(3);
		
		int packetLen = Utils.bytesToInt(bytesBuffer);
		if (packetLen < 6 || packetLen > outputBuffer.getCapacity())
			return DecodeResult.Failure;


		if (inputbuffer.getDataSize() < packetLen)
			return DecodeResult.WantMoreData;

		bytesBuffer[0] = inputbuffer.getAt(packetLen - 2);
		bytesBuffer[1] = inputbuffer.getAt(packetLen - 1);

		int sEnd = Utils.bytesToInt(bytesBuffer);

		if( sEnd != SignatureEnd )
			return DecodeResult.Failure;

		outputBuffer.append(inputbuffer.getBuffer(), 4, packetLen - 6);

		inputbuffer.pop(packetLen);//TODO. verify this is correct.

		return DecodeResult.Content;
	}

	@Override
	public EncodeResult encodeContent(ProtocolContext context,
			Buffer inputBuffer, Buffer outputBuffer) {
		int nTotalSize = 4 + inputBuffer.getDataSize() + 2;

		if (outputBuffer.getRemainingSize() < nTotalSize) {
			return EncodeResult.InsufficientBufferSpace;
		}

		outputBuffer.append(Utils.intToBytes(SignatureStart, 2), 2);
		outputBuffer.append(Utils.intToBytes(nTotalSize, 2), 2);

		outputBuffer.append(inputBuffer);
		outputBuffer.append(Utils.intToBytes(SignatureEnd, 2), 2);


		return EncodeResult.Success;
	}

}
