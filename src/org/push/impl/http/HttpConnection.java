package org.push.impl.http;

import org.push.core.LogicalConnection;
import org.push.core.ServerImpl;
import org.push.core.Common.Login;
import org.push.core.Common.LoginData;
import org.push.protocol.IncomingPacket;

public abstract class HttpConnection extends LogicalConnection {
	
	private static final String HTTP_VERSION_1_0 = "HTTP/1.0";
	private static final String HTTP_VERSION_1_1 = "HTTP/1.1";

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

		process(httpReq, httpResp);
		
		// send back response
		pushPacket(httpResp);
	}

	@Override
	protected Login processLogin(LoginData loginData) {
		return Login.AcceptClientAndRouteRequest;
	}
	
	protected void process(HttpRequest request, HttpResponse response) {
		if (!checkVersion(request, response)) {
			return;
		}
		
		// Version
		response.setVersion(request.getVersion());

		String strMethod = request.getMethod();
		// No Method
		if (strMethod == null || "".equals(strMethod)) {
			response.setStatus(HttpStatus.BadRequest);
			return;
		}

		// Parse the parameters
		if (HttpRequest.METHOD_GET.equalsIgnoreCase(strMethod)) {
			parseGetParameters(request);
		} else if (HttpRequest.METHOD_POST.equalsIgnoreCase(strMethod)) {
			parsePostParameters(request);
		} else {
			response.setStatus(HttpStatus.MethodNotAllowed);
			return;
		}
	}
	
	/**
	 * Check the version in the request, and if the version is
	 * invalid, a corresponding response will be given.
	 * @param request
	 * @param response
	 * @return true if the version is valid
	 */
	private boolean checkVersion(HttpRequest request, HttpResponse response) {
		String strVersion = request.getVersion();
		if (strVersion == null || "".equals(strVersion)) {
			response.setStatus(HttpStatus.BadRequest);
			response.setVersion(HTTP_VERSION_1_1);
			return false;
		}
		
		if (!HTTP_VERSION_1_1.equals(strVersion) &&
				!HTTP_VERSION_1_0.equals(strVersion)) {
			response.setStatus(HttpStatus.HTTPVersionNotSupported);
			response.setVersion(HTTP_VERSION_1_1);
			return false;
		}
		
		return true;
	}
	
	private void parseGetParameters(HttpRequest request) {
		
	}
	
	private void parsePostParameters(HttpRequest request) {
		
	}

}
