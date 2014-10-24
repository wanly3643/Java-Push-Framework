package org.push.core;

import org.push.protocol.IncomingPacket;
import org.push.util.Utils;


/**
 * Skeletal implementation of handler to process the request.
 * 
 * @author Lei Wang
 */

public abstract class Service {

	private Server server;
	
	public Service(Server server) {
		Utils.nullArgCheck(server, "server");
		this.server = server;
	}
	
	public Server getServer() { return server; }
	
	public void setServer(Server server) {
		this.server = server;
	}

    public abstract void handle(LogicalConnection client, 
    		IncomingPacket request);

    public abstract int getRoutingId();

    public abstract String getName();
}
