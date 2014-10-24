package org.push.protocol;

import org.push.protocol.ErrorCodes.DeserializeResult;
import org.push.protocol.ErrorCodes.SerializeResult;

/**
 * This class contains the logic of serialization/de-serialization of messages. 
 * Messages are serialized to bytes before they are sent to protocol then to 
 * network. When protocol decodes the received bytes, it extracts bytes related 
 * to content and send them here for de-serialization.
 * 
 * De-serialization will transform the content bytes into a structured object 
 * that business code can act upon.
 * 
 * Serialization takes a message object as input and produces bytes.
 * 
 * If you are planning to use only one protocol, then you can implement this 
 * class inside your protocol project.
 * However, if you plan to use many protocols, then implement this at the 
 * server level.
 * 
 * @author Lei Wang
 */

public abstract class MessageFactory {

    public abstract SerializeResult serializeMessage(
            OutgoingPacket outgoingPacket, Buffer buffer);

    public abstract DeserializeResult deserializeMessage(Buffer contentBytes, 
    		DeserializeData deserializeData);

    public abstract void disposeIncomingPacket(IncomingPacket packet);

    public abstract void disposeOutgoingPacket(OutgoingPacket packet);

    public boolean preSerializeMessage(OutgoingPacket outgoingPacket) {
        return true;
    }

    public int getMaximumMessageSize() {
		return 8024;
    }
}
