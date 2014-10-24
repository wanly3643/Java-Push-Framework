package org.push.util;


/**
 * It is is debug utility. Use the method {@link #debug(String)} to
 * print the debug information. When the {@link #isDebug} is false,
 * the debug information will not be printed.
 * 
 * @author Lei Wang
 */

public class Debug {
	private static boolean isDebug = true;

	public static void debug(String info) {
		if (isDebug) {
			System.out.println(info);
		}
	}
}
