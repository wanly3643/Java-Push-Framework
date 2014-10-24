package test.server;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.push.core.ListenerOptions;
import org.push.core.QueueOptions;
import org.push.impl.xml.XMLSerializer;

public class TestServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLSerializer xmlSerializer = new XMLSerializer();
		SimpleProtocol simpleProtocol = new SimpleProtocol();
		
		MyServer theServer = new MyServer();
		// For debug, set timeout to 1 hour.
		theServer.getImpl().getServerOptions().setLoginExpireDuration(3600);

		theServer.setMessageFactory(xmlSerializer);

		ListenerOptions lOptions = new ListenerOptions();
		lOptions.setProtocol(simpleProtocol);

		theServer.createListener(10010, lOptions);

		QueueOptions qOptions = new QueueOptions();
		qOptions.setPriority(10);
		qOptions.setPacketsQuota(4);

		theServer.createQueue("queue1", qOptions);

		DemoPublisher publisher = new DemoPublisher(theServer);

		if (!theServer.start(true)) {
			System.out.println("Failed to start server");
			return;
		}

		org.push.util.Utils.sleep(1000L, TimeUnit.MILLISECONDS);
		publisher.start();
		
		Scanner scanner = new Scanner(System.in);

		String cmd;
		while (true) {
			cmd = scanner.next();

			if ("q".equals(cmd)) {
				break;
			}
		}

		publisher.stop();
		theServer.stop();
	}

}
