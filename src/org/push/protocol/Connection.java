package org.push.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class provides a skeletal implementation of the connection 
 * between the server and client so it could be used both on client 
 * side and server side.
 *  
 * @author Lei Wang
 */

public abstract class Connection {

	private boolean isConnectionInitialized;
    private Map<Protocol, ProtocolContext> protocolContextMap;

    public Connection() {
        isConnectionInitialized = false;
        protocolContextMap = new HashMap<Protocol, ProtocolContext>();
    }

    /**
     * Store all the <code>Protocol</code> followed by the current
     * protocol associated with this type of connection with the 
     * <code>ProtocolContext</code>. 
     * To get current protocol, @see {@link getProtocol}
     * @return true if all the protocols are stored, otherwise false
     */
    public boolean setUpProtocolContexts() {
		Protocol protocol = getProtocol();

		ProtocolContext context;
		while (protocol != null) {
            context = protocol.borrowObject();
			if (context == null) {
				return false;
			}

            protocolContextMap.put(protocol, context);

            // Move to next protocol
			protocol = protocol.getLowerProtocol();
		}

		return true;
    }
    
    /**
     * Get the <code>ProtocolContext</code> associated with the
     * protocol.
     * @param protocol   The protocol
     * @return  The <code>ProtocolContext</code> associated or null
     */
    public ProtocolContext getProtocolContext(Protocol protocol) {
        return protocolContextMap.get(protocol);
    }

    /**
     * 
     * @param outgoingBytes
     * @return
     */
    public Protocol advanceInitialization(Buffer outgoingBytes) {
		Protocol currentProtocol = getProtocol().getLowestProtocol();

		// Clear the buffer for use later
		outgoingBytes.clearBytes();

		ProtocolContext context;
		while (currentProtocol != null) {
			context = getProtocolContext(currentProtocol);

			if (!context.isInitializationStarted()) {
				context.setInitializationStarted();
				currentProtocol.startSession(context, outgoingBytes);

				if (!outgoingBytes.isEmpty()) {
					return currentProtocol;
				}
			}

			if (!context.isInitialized()) {
				return null;
			}

			currentProtocol = currentProtocol.getUpperProtocol();
		}
		
		return null;
    }

    /**
     * Check and initialize the connection if it is not initialized.
     * If the <code>ProtocolContext</code> of current protocol is
     * not initialized, the connection will not be initialized.
     */
    public void checkConnectionInitialization() {
		if (isConnectionInitialized) {
			return;
		}

		ProtocolContext context = getProtocolContext(getProtocol());
		if (!context.isInitialized()) {
			return;
		}

		isConnectionInitialized = true;

		initializeConnection();
    }

    /**
     * Release the associated resource of this connection for reuse. 
     * Here it will release the <code>Protocol</code> and 
     * <code>ProtocolContext</code> .
     */
    public void recycle() {
		isConnectionInitialized = false;

        Protocol protocol;
        ProtocolContext context;
		for (Entry<Protocol, ProtocolContext> entry : 
                protocolContextMap.entrySet()) {
			protocol = entry.getKey();
			context = entry.getValue();

			// Release the protocol context
			protocol.returnObject(context);
		}

		protocolContextMap.clear();
    }

    /**
     * Returned the protocol associated with this connection.
     * @see {@link Protocol}
     * @return  the protocol associated or null
     */
	public abstract Protocol getProtocol();

    /**
     * Returned the <code>MessageFactory</code> associated.
     * @see {@link MessageFactory}
     * @return  the <code>MessageFactory</code> associated or null
     */
	public abstract MessageFactory getMessageFactory();

	/**
	 * Do the initialization work for the connection
	 */
	protected abstract void initializeConnection();
}
