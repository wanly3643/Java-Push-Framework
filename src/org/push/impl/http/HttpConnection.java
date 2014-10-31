package org.push.impl.http;

import org.push.core.LogicalConnection;
import org.push.core.ServerImpl;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.protocol.IncomingPacket;

public abstract class HttpConnection extends LogicalConnection {

	public HttpConnection(ServerImpl serverImpl) {
		super(serverImpl);
	}

	@Override
	protected void recycle() {
		;
	}

	@Override
	public void handleRequest(IncomingPacket request) {
		if (!(request instanceof HttpRequest)) {
			return;
		}
		
		HttpRequest httpReq = (HttpRequest)request;
		
		HttpResponse httpResp = new HttpResponse();
		
		// version;
		httpResp.setVersion(httpReq.getVersion());

		process(httpReq, httpResp);
		
		// send back response
		pushPacket(httpResp);
	}

	@Override
	protected Login processLogin(LoginData loginData) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected abstract void process(HttpRequest request, HttpResponse response);

}
