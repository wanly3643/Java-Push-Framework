package org.push.impl.http;

import java.util.HashMap;
import java.util.Map;

import org.push.protocol.IncomingPacket;

public class HttpRequest implements IncomingPacket {
	
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";

	private String method;
	private String url;
	private String version;
	
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> params = new HashMap<String, String>();
	
	private HttpEntity entity;
	
	HttpRequest() {
		;
	}

	public String getMethod() { return this.method; }
	
	public void setMethod(String method) { this.method = method; }

	public String getUrl() { return this.url; }
	
	public void setUrl(String url) { this.url = url; }

	public String getVersion() { return this.version; }
	
	public void setVersion(String version) { this.version = version; }
	
	public Map<String, String> getHeaders() { return this.headers; }
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public Map<String, String> getParameters() { return this.params; }
	
	void setParameters(Map<String, String> params) {
		this.params = params;
	}
	
	public HttpEntity getEntity() { return this.entity; }
	
	public void setEntity(HttpEntity entity) { this.entity = entity; }
}
