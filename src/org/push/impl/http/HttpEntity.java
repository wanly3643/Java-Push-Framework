package org.push.impl.http;

public class HttpEntity {

	private byte[] bytes;
	public HttpEntity() {
	}
	
	public byte[] getBytes() { return this.bytes; }
	
	public void setBytes(byte[] bytes) { this.bytes = bytes; }
}
