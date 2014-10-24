package org.push.measures;

/**
 * 
 * @author Lei Wang
 */

public abstract class Measure {

	protected String name;

	public Measure(String name) {
		this.name = name;
	}

	public abstract void addObservation(MeasureArgs args);
	public abstract String collectAndReset(String timeStamp);
}
