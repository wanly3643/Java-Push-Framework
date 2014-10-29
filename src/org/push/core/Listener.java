package org.push.core;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.push.protocol.Protocol;
import org.push.util.Debug;
import org.push.util.Utils;

/**
 * This class provides a skeletal implementation for listening
 * and accepting socket.
 * 
 * @author Lei Wang
 */

public abstract class Listener {

	protected ServerImpl serverImpl;

	private int uPort;

	protected ListenerOptions listenerOptions;
	
	// The server socket to listen to the port
	private PushServerSocket serverSocket;

	 // If listener is running.
	private volatile boolean blnRunning;
	
	// Thread to accept the client socket
	private Thread acceptorThread;

	public Listener(ServerImpl serverImpl) {
		Utils.nullArgCheck(serverImpl, "serverImpl");
		this.serverImpl = serverImpl;
	    uPort = 0;
	    listenerOptions = new ListenerOptions();
	}
	
	public ServerImpl getServerImpl() { return serverImpl; }
	
	public int getListeningPort() { return this.uPort; }
	
	public void setListeningPort(int uPort) {
		Utils.unsignedIntArgCheck(uPort, "uPort");
		this.uPort = uPort;
	}

	public boolean startListening() {
		// If already running
		if (blnRunning) {
			return true;
		}

		InetSocketAddress serverAddr = null;
		String interfaceAddress = getOptions().getInterfaceAddress();
		if(interfaceAddress != null) {
			if (!"255.255.255.255".equals(interfaceAddress)) {
				return false;
			}

			serverAddr = new InetSocketAddress(interfaceAddress, 
					getListeningPort());
	    } else {
			serverAddr = new InetSocketAddress(getListeningPort());
	    }

	    blnRunning = doListening(serverAddr);
	 
	    return blnRunning;
	}

	public void stopListening() {
		blnRunning = false;

	    // Close server socket
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
		
		// Wait the thread to terminate
		if (acceptorThread != null) {
			try {
				acceptorThread.join();
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}

			acceptorThread = null;
		}
	}
	
	private boolean doListening(InetSocketAddress serverAddr) {
		boolean ret = false;
		int socketBufferSize = getServerImpl().getServerOptions()
					.getSocketBufferSize();
		int socketType = getServerImpl().getServerOptions()
					.getSocketType();
		try {
			// Create
			serverSocket = SocketFactory.getDefault().createServerSocket(
					socketType, socketBufferSize);
			
			// Bind
			serverSocket.bind(serverAddr);

			Debug.debug("Start to listen " + serverAddr.getHostName() + ":"
					+ serverAddr.getPort());
			
			// Accept
			doAccept();
			
			ret = true;
		} catch (IOException e) {
			e.printStackTrace();
			if (serverSocket != null) {
				serverSocket.close();
				serverSocket = null;
			}
		}
		
		return ret;
	}
	
	private void doAccept()
	{
		// Start a new thread
		acceptorThread = new Thread(new Runnable()  {
			public void run() {
				while (blnRunning) {
					try {
						PushClientSocket clientSocket = serverSocket.accept();
						
						Debug.debug("New client from " + clientSocket.getIP());

						// Start servicing the client connection
						if (!handleAcceptedSocket(clientSocket)) {
							clientSocket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				}
			}
			
		});
		
		// Start the thread
		acceptorThread.start();
	}

	public void setOptions(ListenerOptions listenerOptions) {
		Utils.nullArgCheck(listenerOptions, "listenerOptions");
		this.listenerOptions = listenerOptions;
	}
	
	public ListenerOptions getOptions() {
		return listenerOptions;
	}

	public Protocol getProtocol() { return listenerOptions.getProtocol(); }
	
	protected abstract boolean handleAcceptedSocket(
			PushClientSocket clientSocket);
}
