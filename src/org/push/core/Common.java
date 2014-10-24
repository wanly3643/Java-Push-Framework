package org.push.core;

import org.push.protocol.IncomingPacket;
import org.push.protocol.OutgoingPacket;
import org.push.util.CppEnum;

/**
 * Some common using enumeration.
 * 
 * @author Lei Wang
 */

public class Common {

	public static enum SendResult implements CppEnum {
		OK(0),
		Retry(1),
		NotOK(2);

        private int value;

        private SendResult(int value) { this.value = value; }

        public int value() { return this.value; }
	}

	public static enum DisconnectionReason implements CppEnum {
		PeerClosure(0),
		InactiveClient(1),
		ForceableClosure(2),
		RequestedClosure(3),
		UnknownFailure(4);

        private int value;

        private DisconnectionReason(int value) { this.value = value; }

        public int value() { return this.value; }
	}

	public static enum Login implements CppEnum {
		RefuseAndWait(0),
		RefuseAndClose(1),
		AcceptClient(2),
		AcceptClientAndRouteRequest(2);

        private int value;

        private Login(int value) { this.value = value; }

        public int value() { return this.value; }

		public static boolean IsSucceeded(Login type) {
			return type == AcceptClientAndRouteRequest || type == AcceptClient;
		}
	}

	public static class LoginData {
		private IncomingPacket pRequest;
		private OutgoingPacket pResponse;
		private ConnectionContext connectionContext;
		private String clientKey;

		public LoginData() {
			pRequest = null;
			pResponse = null;
			connectionContext = null;
			clientKey = "";
		}
		
		public IncomingPacket getRequest() { return this.pRequest; }

		public OutgoingPacket getResponse() { return this.pResponse; }
		
		public ConnectionContext getConnectionContext() {
			return this.connectionContext;
		}

		public String getClientKey() { return this.clientKey; }

		public void setRequest(IncomingPacket pRequest) {
			this.pRequest = pRequest;
		}

		public void setResponse(OutgoingPacket pResponse) {
			this.pResponse = pResponse;
		}

		public void setConnectionContext(ConnectionContext connectionContext) {
			this.connectionContext = connectionContext;
		}

		public void setClientKey(String clientKey) {
			this.clientKey = clientKey;
		}
		
	};
}
