package org.push.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.push.util.Utils;

/**
 * Implementation of <code>PushClientSocket</code> by Java NIO.
 * 
 * @author Lei Wang
 */

public class PushClientSocketImpl implements PushClientSocket {

	private SocketChannel channel;
	private Socket socket;
	
	private int socketBufferSize;
	private ByteBuffer iBuf;
	private ByteBuffer oBuf;
	
	PushClientSocketImpl(SocketChannel channel, int socketBufferSize) 
		throws IOException {
		Utils.unsignedIntArgCheck(socketBufferSize, "socketBufferSize");
		this.channel = channel;
		channel.configureBlocking(false);
		this.socket = channel.socket();
		this.socketBufferSize = socketBufferSize;
		this.iBuf = ByteBuffer.allocate(socketBufferSize);
		this.oBuf = ByteBuffer.allocate(socketBufferSize);
	}

	public String getIP() {
		InetAddress addr = socket.getInetAddress();
		
		if (addr != null) {
			return addr.getHostAddress();
		}
		
		return null;
	}

	public int getPort() {
		return socket.getPort();
	}

	public SelectionKey registerSelector(Selector selector, int ops, 
			Object attachment) throws IOException {
		selector.wakeup(); // To prevent block when calling register method
		return channel.register(selector, ops, attachment);
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	public int send(byte[] buffer, int offset, int size) throws IOException {
		Utils.unsignedIntArgCheck(size, "size");
		Utils.unsignedIntArgCheck(offset, "offset");
		
		if (size == 0) {
			return 0;
		}

		int maxSize = Math.min(socketBufferSize, size);
		oBuf.clear();
		oBuf.put(buffer, offset, maxSize);
		oBuf.flip();
		
		return channel.write(oBuf);
	}

	public int recv(byte[] buffer, int offset, int size) throws IOException {
		Utils.unsignedIntArgCheck(size, "size");
		
		if (size == 0) {
			return 0;
		}

		int maxSize = Math.min(socketBufferSize, size);
		if (maxSize <= 0) {
			return 0;
		}

		iBuf.clear();
		iBuf.limit(maxSize);
		
		int recvSize = channel.read(iBuf);
		if (recvSize > 0) {
			System.arraycopy(iBuf.array(), 0, buffer, 
					offset, recvSize);
		}

		return recvSize;
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public boolean isConnected() {
		return socket.isConnected();
	}
}
