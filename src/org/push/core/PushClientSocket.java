package org.push.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * A common interface of the client socket.
 * The default implementation is <code>PushClientSocketImpl</code>
 * 
 * @author Lei Wang
 */

public interface PushClientSocket {

	public String getIP();
	public int getPort();
	
	public SelectionKey registerSelector(Selector selector, int ops, 
			Object attachment) throws IOException;
	
	public int send(byte[] buffer, int offset, int size) throws IOException;
	
	public int recv(byte[] buffer, int offset, int size) throws IOException;
	
	public boolean isOpen();
	
	public boolean isConnected();
	
    public void close();
}
