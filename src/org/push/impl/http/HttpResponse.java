package org.push.impl.http;

import java.util.HashMap;
import java.util.Map;

import org.push.protocol.OutgoingPacket;

public class HttpResponse implements OutgoingPacket {

	private HttpStatus status;
	private String version;
	
	private Map<String, String> headers = new HashMap<String, String>();
	
	private byte[] entity;
	
	HttpResponse() { }

	public HttpStatus getStatus() { return this.status; }
	
	public void setStatus(HttpStatus status) { this.status = status; }

	public String getVersion() { return this.version; }
	
	public void setVersion(String version) { this.version = version; }
	
	public Map<String, String> getHeaders() { return this.headers; }
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public byte[] getEntity() { return this.entity; }
	
	public void setEntity(byte[] entity) { this.entity = entity; }
}
