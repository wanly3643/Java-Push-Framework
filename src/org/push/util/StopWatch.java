package org.push.util;

/**
 * It represents a stop watch. It will start return how many
 * seconds has passed by from you create this object or calling
 * {@link #reset()} by calling {@link #getElapsedTime()} 
 * 
 * @author Lei Wang
 */

public class StopWatch {
	// Use nano seconds here
	private static final double QPFrequency = 1000000.00;
	private long mStartCounter;
	private long mLastCounter;

	public StopWatch() {
		mStartCounter = System.nanoTime();
	    reset();
	}

	/**
	 * Reset for next use
	 */
	public void reset() {
		mLastCounter = System.nanoTime();
	}

	/**
	 * How many seconds has passed.
	 * @see #reset()
	 * @return  How many seconds
	 */
	public double getElapsedTime() {
		return getElapsedTime(true);
	}

	/**
	 * How many seconds has passed.
	 * @see #reset()
	 * @param blnStart If true, a reset will be called automatically
	 * @return How many seconds
	 */
	public double getElapsedTime(boolean blnStart) {
	    long curCounter = System.nanoTime();
	    long mElapsedTime = (curCounter - (blnStart ? mStartCounter :  mLastCounter));

	    if(!blnStart) {
	        reset();
	    }

	    return (double)mElapsedTime / QPFrequency;
	}
}
