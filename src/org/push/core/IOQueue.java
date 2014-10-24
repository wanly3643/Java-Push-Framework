package org.push.core;

/**
 * Interface to manage the socket I/O event
 * 
 * @author Lei Wang
 */

public interface IOQueue<T> {

	public boolean create();

	public void free();

	public IOEvent<T> getQueuedEvent(boolean isInputEvents);

	public boolean addSocketContext(PushClientSocket socket, T context);

	public void deleteSocketContext(PushClientSocket socket);

	public boolean rearmSocketForWrite(PushClientSocket socket, T context);

	public boolean rearmSocketForRead(PushClientSocket socket, T context);
}
