/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.push.util;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Common utility
 * @author Lei Wang
 */

public class Utils {
	
	/**
	 * The String format used when printing timestamp
	 */
	private static final SimpleDateFormat DEFAULT_FORMAT = 
		new SimpleDateFormat("yyyy-mm-dd HH:MM:SS");

	/**
	 * The charset used to convert between String and byte
	 */
	public static final String charsetName = "UTF-8";

	/**
	 * How many bits a byte contain when convert between 
	 * integer and byte array.
	 * @see {@link #intToBytes(int, int)}
	 * @see {@link #bytesToInt(byte[], int)}
	 */
	private static final byte byteLength = 8;

	/**
	 * String format of now.
	 * @return  The text of time.
	 */
	public static String getCurrentTime() {
		return DEFAULT_FORMAT.format(new Date());
	}
	
	/**
	 * Same as the C++ function "difftime". Return how many seconds there
	 * are between the two time of the arguments.
	 * Note: the argument is by milliseconds but the return is by seconds.
	 * @param now
	 * @param before
	 * @return
	 */
	public static double diffTime(long now, long before) {
		return (now - before) / (1000.00d);
	}
	
	/**
	 * How many seconds has been past since the time by
	 * the argument.
	 * @see #diffTime(long, long)
	 * @param before
	 * @return
	 */
	public static double timePassed(long before) {
		return diffTime(System.currentTimeMillis(), before);
	}

	/**
	 * Convert the integer into byte array.
	 * @param number  The integer
	 * @param length  The length of the byte array returned
	 * @return the byte array
	 */
    public static byte[] intToBytes(int number, int length) {  
        byte[] byteArray = new byte[length];

        int shiftNum = 0;
        for(int i = 0; i < length; i ++)
        {
            shiftNum = (length - i - 1) * byteLength;
            byteArray[i] = (byte)((number >> shiftNum) & 0xFF);
        }
        return byteArray;
    }

    /**
     * Convert the byte array into integer.
     * @see {@link #intToBytes(int, int)}
     * @param bytes  the byte array
     * @return  The integer
     */
    public static int bytesToInt(byte[] bytes) {
        int ret = 0x00;

        int length = bytes.length;
        int shiftNum = 0;
        for(int i = 0; i < length; i ++) {
            shiftNum = (length - i - 1) * byteLength;
            ret |= (int)((bytes[i] & 0xFF) << shiftNum);
        }
        return ret;
    }
	
	public static void sleep(long duration, TimeUnit unit) {
		long millis = unit.toMillis(duration);
		int nanos = 0;

		if (unit == TimeUnit.MICROSECONDS) {
			nanos = (int)(TimeUnit.MICROSECONDS.toNanos(duration) - 
					TimeUnit.MILLISECONDS.toNanos(millis));
		} else if  (unit == TimeUnit.NANOSECONDS) {
			nanos = (int)(duration - TimeUnit.MILLISECONDS.toNanos(millis));
		} else {
			// Nothing will be done
		}

		try {
			Thread.sleep(millis, nanos);
		} catch (InterruptedException e) {
			// Ignore;
		}
	}

    /**
     * It will check if the value is an unsigned integer.
     * If not, an IllegalArgumentException will be thrown
     * @param value
     * @param argName 
     */
    public static void unsignedIntArgCheck(int value, String argName) 
            throws IllegalArgumentException {
        
        if (value < 0) {
            throw new IllegalArgumentException(argName + " cannot be negative");
        }
    }
    /**
     * It will check if the value is null.
     * If null, an NullPointerException will be thrown
     * @param value
     * @param argName 
     */
    public static void nullArgCheck(Object value, String argName) 
            throws NullPointerException {
        
        if (value == null) {
            throw new NullPointerException(argName + " cannot be null");
        }
    }
    
    public static byte[] stringToBytes(String str) {
    	try {
			return str.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
    }
    
    public static String bytesToString(byte[] bytes, int length) {
    	try {
			return new String(bytes, 0, length, charsetName);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
    }
}
