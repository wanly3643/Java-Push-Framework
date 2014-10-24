package test.client;

import java.util.Scanner;

import org.push.impl.xml.XMLPacket;
import org.push.impl.xml.XMLSerializer;
import org.push.monitor.AnalyticsProtocol;

import test.server.SimpleProtocol;

public class DemoClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ClientSocket client = new ClientSocket();
		SimpleProtocol simpleProtocol = new SimpleProtocol();
		XMLSerializer xmlSerializer = new XMLSerializer();

		if (!client.initialize(simpleProtocol, xmlSerializer)) {
			System.exit(-1);
		}

		client.registerHandler(1, client);

		if (!client.connect("localhost", 10010))
		{
			System.exit(-1);
		}


		System.out.println("Press q to quit, s to subscribe all connected clients to real time, e to trigger echo");
		Scanner scanner = new Scanner(System.in);
		
		String cmd;
		do 
		{
			cmd = scanner.next();
			cmd = cmd.toUpperCase();

			if ("S".equals(cmd)) {
				XMLPacket message = new XMLPacket(
						AnalyticsProtocol.LoginRequest);
				message.setArgumentAsText("arg1", "subscribe");

				client.sendRequest(message);
			} else if ("E".equals(cmd)) {
				XMLPacket message = new XMLPacket(
						AnalyticsProtocol.LoginRequest);
				message.setArgumentAsText("arg1", "echo");
				client.sendRequest(message);
			}

		} while (!"Q".equals(cmd));


		client.disconnect(true);
	}

}
