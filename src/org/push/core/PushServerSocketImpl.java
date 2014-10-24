package org.push.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

/**
 * Implementation of <code>PushServerSocket</code> by Java NIO.
 * 
 * @author Lei Wang
 */

public class PushServerSocketImpl implements PushServerSocket {

	private ServerSocket serverSocket;
	private ServerSocketChannel serverChannel;
	private int socketBufferSize;

	PushServerSocketImpl(int socketBufferSize) throws IOException {
		serverChannel = ServerSocketChannel.open();
		serverSocket = serverChannel.socket();
		this.socketBufferSize = socketBufferSize;
	}

	public void bind(InetSocketAddress serverAddr) throws IOException {
		serverSocket.bind(serverAddr);

	}

	public PushClientSocket accept() throws IOException {
		return new PushClientSocketImpl(serverChannel.accept(), 
				socketBufferSize);
	}

	public void close() {
		try {
			serverChannel.close();
		} catch (IOException e) {
			// Ignore
		}

	}

}
