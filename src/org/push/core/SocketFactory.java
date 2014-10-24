package org.push.core;

import java.io.IOException;

/**
 * A factory to create <code>PushServerSocket</code> object
 * 
 * @author Lei Wang
 */

public class SocketFactory {

	private static final SocketFactory singleton = 
		new SocketFactory();

	private SocketFactory() { }
	
	public static SocketFactory getDefault() {
		return singleton;
	}
	
	public PushServerSocket createServerSocket(int option, 
			int socketBufferSize) throws IOException {
		return new PushServerSocketImpl(socketBufferSize);
	}
}
