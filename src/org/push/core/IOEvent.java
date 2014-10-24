package org.push.core;

/**
 * Event of socket I/O.
 * 
 * @author Lei Wang
 */

public class IOEvent<T> {

	public static enum IOEventType {
		read,
		write;
	}
	
	private IOEventType type;
	private T context;

	IOEvent(IOEventType type, T context) {
		this.type = type;
		this.context = context;
	}
	
	public IOEventType type() {
		return this.type;
	}
	
	public T context() {
		return this.context;
	}
}
