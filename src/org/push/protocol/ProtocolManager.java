package org.push.protocol;

import org.push.protocol.ErrorCodes.DecodeResult;
import org.push.protocol.ErrorCodes.DeserializeResult;
import org.push.protocol.ErrorCodes.EncodeResult;
import org.push.protocol.ErrorCodes.NetworkDeserializeResult;
import org.push.protocol.ErrorCodes.NetworkSerializeResult;
import org.push.protocol.ErrorCodes.SerializeResult;


/**
 * This class uses <code>MessageFactory</code> to serialize and 
 * de-serialize message by the protocol list defined.
 * 
 * @author Lei Wang
 */

public class ProtocolManager {

    public ProtocolManager() { }

    public NetworkSerializeResult serializeOutgoingPacket(
            Connection connection, OutgoingPacket outgoingMsg, 
            Buffer outputBuffer, SerializeData serializeData) {
        
		MessageFactory msgFactory = connection.getMessageFactory();

		// First encode the message:
		RecyclableBuffer packetBuffer = new RecyclableBuffer();
		SerializeResult ret = msgFactory.serializeMessage(outgoingMsg, 
                packetBuffer);
		if (ret != SerializeResult.Success) {
			return NetworkSerializeResult.convertSerializeFailure(ret);
        }

		NetworkSerializeResult ret2 = serializeOutgoinBytes(connection, 
                packetBuffer, outputBuffer, serializeData);
        
        // Release the buffer
        packetBuffer.release();
        
        return ret2;
    }

    public NetworkSerializeResult serializeOutgoinBytes(
            Connection connection, Buffer outgoingBytes, 
            Buffer outputBuffer, SerializeData serializeData) {

		Protocol protocol = serializeData.getProtocol();
		if (protocol == null) {
			return outputBuffer.append(outgoingBytes) ? 
                    NetworkSerializeResult.Success : 
                    NetworkSerializeResult.Retry;
		}

		RecyclableBuffer tmpBuffer = new RecyclableBuffer();

		Buffer input = outgoingBytes;
		Buffer output = tmpBuffer;
        Buffer tmpForSwap; 
        EncodeResult encodeResult;
		while (protocol != null) {
			encodeResult = protocol.encodeContent(
                    connection.getProtocolContext(protocol), input, output);

			if (encodeResult != EncodeResult.Success)
			{
                // Release the buffer
                tmpBuffer.release();

				if (encodeResult == EncodeResult.InsufficientBufferSpace 
                            && (protocol.getLowerProtocol() != null)) {

                    // Buffer overflow at an intermediate protocol 
                    // layer is not allowable.
					return NetworkSerializeResult.Failure;
                }

				return NetworkSerializeResult.convertEncodingFailure(
                        encodeResult);
			}

            /* Use output as input for next protocol */
			tmpForSwap = input;
			input = output;
			output = tmpForSwap;

			output.clearBytes();

			protocol = protocol.getLowerProtocol();
		}
        
        // Save the output into the buffer
        outputBuffer.append(tmpBuffer);
        
        // Release the buffer
        tmpBuffer.release();

		return NetworkSerializeResult.Success;
    }

    public NetworkDeserializeResult tryDeserializeIncomingPacket(
            Connection connection, DeserializeData deserializeData) {
        
		MessageFactory msgFactory = connection.getMessageFactory();
		RecyclableBuffer tmpOutputBuffer = new RecyclableBuffer();
		
        Protocol currentProtocol;
        Protocol upperProtocol;
        ProtocolContext currentContext;
        Buffer outputBuffer;
        DecodeResult decodeResult;
		while (true) {
			currentProtocol = deserializeData.getProtocol();
			currentContext = connection.getProtocolContext(currentProtocol);

			upperProtocol = currentProtocol.getUpperProtocol();

			outputBuffer = (upperProtocol != null) ? 
                    tmpOutputBuffer : deserializeData.getProtocolBytes();

			// Give chance to protocol to call startSession before processing
            // incoming data.
			if (!currentContext.isInitialized() && 
                    !currentContext.isInitializationStarted()) {
				currentContext.setInitializationStarted();
				currentProtocol.startSession(currentContext, 
                        deserializeData.getProtocolBytes());
				//
				if (deserializeData.getProtocolBytes().hasBytes()) {
					return NetworkDeserializeResult.ProtocolBytes;
				}
			}
			
			decodeResult = currentProtocol.tryDecode(currentContext, 
                    outputBuffer);

			if (decodeResult == DecodeResult.WantMoreData) {
				if (upperProtocol != null) {
					deserializeData.setProtocol(upperProtocol);
					continue;
				} else {
					tmpOutputBuffer.release();
					return NetworkDeserializeResult.WantMoreData;
				}
			} else if (decodeResult == DecodeResult.NoContent) {
				continue;
			} else if (decodeResult == DecodeResult.ProtocolBytes) {
				if (upperProtocol != null) {
					deserializeData.getProtocolBytes().append(tmpOutputBuffer);
				}
				tmpOutputBuffer.release();
				
				//
				return NetworkDeserializeResult.ProtocolBytes;
			} else if(decodeResult == DecodeResult.Close) {
				if (upperProtocol != null) {
					deserializeData.getProtocolBytes().append(tmpOutputBuffer);
				}
				tmpOutputBuffer.release();

				return NetworkDeserializeResult.Close;
			} else if (decodeResult == DecodeResult.Content) {
				if (!currentContext.isInitialized()) {
					return NetworkDeserializeResult.Initializationfailure;
				}

				if (upperProtocol == null) {
					//De-serialize and return message.
					DeserializeResult deserializeResult = 
                            msgFactory.deserializeMessage(outputBuffer, 
                            deserializeData);

					if (deserializeResult == DeserializeResult.DiscardContent) {
						continue;
					} else if (deserializeResult == DeserializeResult.Failure) {
						tmpOutputBuffer.release();
						return NetworkDeserializeResult.Failure;
					} else if(deserializeResult == DeserializeResult.Success) {
						tmpOutputBuffer.release();
						return NetworkDeserializeResult.Content;
					} else {
						//Should not come here.
					}
				}

				//Give content data to upper protocol:
				if (!upperProtocol.readData(connection.getProtocolContext(
                        upperProtocol), outputBuffer)) {
					tmpOutputBuffer.release();
					return NetworkDeserializeResult.Failure;
				}
			} else { //decodeResult == DecodeResult::Failure
				tmpOutputBuffer.release();
				return NetworkDeserializeResult.Failure; //
			}
		}
    }

    public boolean processIncomingData(Connection connection, 
            Buffer incomingBytes) {
		Protocol lowestProtocol = connection.getProtocol().getLowestProtocol();

		return lowestProtocol.readData(connection.getProtocolContext(
                lowestProtocol), incomingBytes);
    }
}
