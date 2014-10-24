package org.push.core;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Common interface of server socket.
 * The default implementation is <code>PushServerSocketImpl</code>
 * 
 * @author Lei Wang
 */

public interface PushServerSocket {

	public void bind(InetSocketAddress serverAddr) throws IOException;

	public PushClientSocket accept() throws IOException;

	public void close();
}
