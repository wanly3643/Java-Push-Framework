package test.client;

import org.push.protocol.IncomingPacket;

public interface ResponseHandler {

	public void handleResponse(IncomingPacket _packet);
}
