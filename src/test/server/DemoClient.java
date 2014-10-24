package test.server;

import org.push.core.LogicalConnection;
import org.push.core.ServerImpl;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.impl.xml.XMLPacket;
import org.push.monitor.AnalyticsProtocol;
import org.push.protocol.IncomingPacket;

public class DemoClient extends LogicalConnection {

	public DemoClient(ServerImpl serverImpl) {
		super(serverImpl);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void recycle() {
		// TODO Auto-generated method stub

	}

	@Override
	protected Login processLogin(LoginData loginData) {
		return Login.AcceptClientAndRouteRequest;
	}

	@Override
	public void handleRequest(IncomingPacket request) {
		XMLPacket message = (XMLPacket)request;

		String requestText = message.getArgumentAsText("arg1");
		if ("echo".equals(requestText)) {
			XMLPacket reply = new XMLPacket(AnalyticsProtocol.LoginRequest);

			reply.setArgumentAsText("text", "hello");
			pushPacket(reply);
		} else if ("subscribe".equals(requestText)) {
			subscribeToQueue("queue1");
		}
	}
}
