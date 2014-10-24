package org.push.core;

/**
 * Facade of <code>DemuxImpl</code>.
 * 
 * @author Lei Wang
 */

public class Demultiplexor {

	private DemuxImpl impl;
	
	public Demultiplexor(ServerImpl serverImpl) {
	    impl = new DemuxImpl(this, serverImpl);
	}

	public boolean start() {
		return impl.start();
	}

	public void stop() {
		impl.stop();
	}
}
