package test.server;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import org.push.impl.xml.XMLPacket;
import org.push.monitor.AnalyticsProtocol;

public class DemoPublisher {
	public static final int nMaximumMessageSize = 100;
	private Thread thread;
	
	private MyServer theServer;
	
	private volatile boolean isRunning;
	
	public DemoPublisher(MyServer theServer) {
		this.theServer = theServer;
	}

	public void start() {
		isRunning = true;
		thread = new Thread(new Runnable() {

			public void run() {
				doJob();
			}
			
		});
		
		thread.start();
	}

	public void stop() {
		isRunning = false;
		
		if (thread != null) {
			LockSupport.unpark(thread);
			
			try {
				thread.join();
			} catch (InterruptedException e) {
				// ignore
			}
			
			thread = null;
		}
	}

	public void doJob() {
		long nanos = 10000000000L; // 10 seconds
		int counter = 1;
		while (isRunning) {
			LockSupport.parkNanos(nanos);
			
			// stop
			if (!isRunning) {
				break;
			}

			//
			XMLPacket response = new XMLPacket(AnalyticsProtocol.LoginRequest);
			String text = genRandom();
			response.setArgumentAsText("text", text);
			response.setArgumentAsInt("hash", text.hashCode());
			response.setArgumentAsInt("id", ++counter);

			//System.out.println("Broadcasting ....");
			theServer.pushPacket(response, "queue1");
		}
	}
	
	private static String genRandom() {
		String alphanum =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

		int size = alphanum.length() - 1;
		Random r = new Random();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < (nMaximumMessageSize); ++i) {
			sb.append(alphanum.charAt(Math.abs(r.nextInt()) % size));
		}

		return sb.toString();
	}
}
