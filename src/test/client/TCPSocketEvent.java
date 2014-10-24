package test.client;

import org.push.protocol.IncomingPacket;
import org.push.util.CppEnum;

public class TCPSocketEvent {

	public static enum Type implements CppEnum {
        ConnectionEstablished(0),
        ConnectionClosed(1),
        NewMessage(2);

        private int value;

        private Type(int value) { this.value = value; }

        public int value() { return this.value; }
    };

    public Type type;
    public int commandId;
    public IncomingPacket pPacket;
}
