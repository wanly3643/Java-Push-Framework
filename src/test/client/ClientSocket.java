package test.client;

import org.push.impl.xml.XMLPacket;
import org.push.protocol.IncomingPacket;

public class ClientSocket extends TCPSocket implements ResponseHandler {

	public ClientSocket() {
		// TODO Auto-generated constructor stub
	}

	public ClientSocket(boolean relayToUserThread) {
		super(relayToUserThread);
		// TODO Auto-generated constructor stub
	}

	public void handleResponse(IncomingPacket _packet) {
		XMLPacket msg = (XMLPacket) _packet;

		System.out.println("received new server reply: " +
				msg.getArgumentAsText("text"));
	}

	@Override
	protected void onConnected() {
		System.out.println("Connected");
	}

	@Override
	protected void onConnectionClosed() {
		System.out.println("Closed");
	}

}
