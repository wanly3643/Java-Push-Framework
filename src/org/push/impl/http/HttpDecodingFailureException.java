package org.push.impl.http;

public class HttpDecodingFailureException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3146357111549884066L;

	public HttpDecodingFailureException() {
	}

	public HttpDecodingFailureException(String message) {
		super(message);
	}

	public HttpDecodingFailureException(Throwable cause) {
		super(cause);
	}

	public HttpDecodingFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
