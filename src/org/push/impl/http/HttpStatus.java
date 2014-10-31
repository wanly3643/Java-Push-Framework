package org.push.impl.http;

public enum HttpStatus {
	
	BAD_REQUEST(400, "BAD REQUEST");

	private int code;
	private String msg;

	private HttpStatus(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	public int code() { return this.code; }
	
	public String message() { return this.msg; }
}
