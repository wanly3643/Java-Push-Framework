package org.push.core;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.push.util.Debug;

/**
 * Listener implementation to listen and accept socket.
 * 
 * @author Lei Wang
 */

public class ListenerImpl {

	private Listener facade;
	
	// The server socket to listen to the port
	private PushServerSocket serverSocket;

	 // If listener is running.
	private volatile boolean blnRunning;
	
	// Thread to accept the client socket
	private Thread acceptorThread;

	public ListenerImpl(Listener facade) {
	    this.facade = facade;
	}
	
	public PushServerSocket getServerSocket() {
		return this.serverSocket;
	}

	public boolean startListening() {
		// If already running
		if (blnRunning) {
			return true;
		}

		InetSocketAddress serverAddr = null;
		String interfaceAddress = facade.getOptions().getInterfaceAddress();
		if(interfaceAddress != null) {
			if (!"255.255.255.255".equals(interfaceAddress)) {
				return false;
			}

			serverAddr = new InetSocketAddress(interfaceAddress, 
					facade.getListeningPort());
	    } else {
			serverAddr = new InetSocketAddress(facade.getListeningPort());
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
		int socketBufferSize = facade.getServerImpl().getServerOptions()
					.getSocketBufferSize();
		int socketType = facade.getServerImpl().getServerOptions()
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
						if (!facade.handleAcceptedSocket(clientSocket)) {
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
}
