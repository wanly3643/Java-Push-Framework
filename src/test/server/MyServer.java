package test.server;

import org.push.core.LogicalConnection;
import org.push.core.Server;

public class MyServer extends Server {
	@Override
	public LogicalConnection createLogicalConnection() {
		return new DemoClient(this.getImpl());
	}

}
