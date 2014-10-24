package org.push.util;

import org.push.exception.InvalidIPAddressException;

import sun.net.util.IPAddressUtil;

/**
 * This class represents a range of IP. It provides methods
 * to check if a specific IP is within the range.
 * It is useful to check the IP of a client.
 * 
 * @author Lei Wang
 */

public class IPRange {

	private long startIP;
	private long endIP;
	private int ipType;
	
	/**
	 * Constructor to create a <code>IPRange</code>. If the argument is 
	 * not valid IP, an <code>InvalidIPAddressException</code> will be
	 * thrown.
	 * 
	 * @param startIP
	 * @param endIP
	 * @throws InvalidIPAddressException
	 */
	public IPRange(String startIP, String endIP) 
		throws InvalidIPAddressException
	{
		if (!isValidIP(startIP)) {
			throw new InvalidIPAddressException("Invalid IP: " + startIP);
		}

		if (!isValidIP(endIP)) {
			throw new InvalidIPAddressException("Invalid IP: " + endIP);
		}

		if (!isSameFormat(startIP, endIP)) {
			throw new InvalidIPAddressException("Not same type of IP:" 
					+ startIP + "/" + endIP);
		}
		
		byte[] ipBytes = text2Numeric(startIP);
		this.ipType = ipBytes.length;
		this.startIP = numeric2Long(ipBytes);
		this.endIP = numeric2Long(text2Numeric(endIP));
	}
	
	/**
	 * Check if the IP value is within the range. Usually the IP is a 
	 * <code>String</code>, the IP could be converted into a byte array
	 * by calling the method {@link #text2Numeric(String)} and a byte array
	 * could be converted into long by {@link #numeric2Long(byte[])
	 * @param ipLong the IP value of long type
	 * @return  true if it is with the range, otherwise false
	 */
	public boolean isWithin(long ipLong) {
        return (ipLong <= endIP && ipLong >= startIP);
	}
	
	/**
	 * Check if the IP value is within the range. If the argument is 
	 * not valid IP, an <code>InvalidIPAddressException</code> will be
	 * thrown.
	 * @see also {@link #isWithin(long)} and {@link #isWithin(byte[])}
	 * @param ip  IP of string format
	 * @return true if it is with the range, otherwise false
	 * @throws InvalidIPAddressException
	 */
	public boolean isWithin(String ip) throws InvalidIPAddressException {
        return (isWithin(text2Numeric(ip)));
	}
	
	/**
	 * Check if the IP value is within the range. If the argument is 
	 * not valid IP, an <code>InvalidIPAddressException</code> will be
	 * thrown.
	 * @see also {@link #isWithin(long)} and {@link #isWithin(String)}
	 * @param ipBytes IP of byte array format
	 * @return true if it is with the range, otherwise false
	 * @throws InvalidIPAddressException
	 */
	public boolean isWithin(byte[] ipBytes) throws InvalidIPAddressException {
		if (ipBytes.length != ipType) {
			return false;
		}

    	long ipLong = numeric2Long(ipBytes);
    	
    	return isWithin(ipLong);
	}

	/**
	 * Convert the value of IP by byte array into long.If the argument is 
	 * not valid IP, an <code>InvalidIPAddressException</code> will be
	 * thrown.
	 * @param ipBytes IP of byte array format
	 * @return the long value of IP
	 * @throws InvalidIPAddressException
	 */
	public static long numeric2Long(byte[] ipBytes)
		throws InvalidIPAddressException
	{
		long lRet = 0L;
		if (ipBytes.length == 4) {
			lRet = (ipBytes[3] & 0xFF) << 24
			     | (ipBytes[2] & 0xFF) << 16
			     | (ipBytes[1] & 0xFF) << 8
			     | (ipBytes[0] & 0xFF);
		} else if (ipBytes.length == 6) {
			lRet = (ipBytes[5] & 0xFF) << 40
		     | (ipBytes[4] & 0xFF) << 32
		     | (ipBytes[3] & 0xFF) << 24
		     | (ipBytes[2] & 0xFF) << 16
		     | (ipBytes[1] & 0xFF) << 8
		     | (ipBytes[0] & 0xFF);
		} else {
			throw new RuntimeException("Unknown IP format");
		}
		
		return lRet;
	}

	/**
	 * Convert the value of IP by String into byte array. If the argument 
	 * is not valid IP, an <code>InvalidIPAddressException</code> will be
	 * thrown.
	 * @param ip The IP by String
	 * @return the byte array of IP
	 * @throws InvalidIPAddressException
	 */
	public static byte[] text2Numeric(String ip) 
		throws InvalidIPAddressException {
		if (IPAddressUtil.isIPv4LiteralAddress(ip)) {
			return IPAddressUtil.textToNumericFormatV4(ip);
		} else if (IPAddressUtil.isIPv6LiteralAddress(ip)) {
			return IPAddressUtil.textToNumericFormatV6(ip);
		} else {
			throw new InvalidIPAddressException("Unknown IP format:" + ip);
		}
	}
	
	/**
	 * Check the IP text is valid
	 * @param ip  the text of IP
	 * @return true if valid, otherwise false
	 */
	public static boolean isValidIP(String ip) {
		if (ip == null) {
			return false;
		}

		return IPAddressUtil.isIPv4LiteralAddress(ip) || 
			   IPAddressUtil.isIPv6LiteralAddress(ip);
	}

	/**
	 * Since there are two types of IP: IPv4 and IPv6, so this
	 * method will check if the two IP are same type.
	 * 
	 * @param ipStart  The text of start IP
	 * @param ipEnd    The text of end IP
	 * @return true if same type, otherwise false
	 */
	private static boolean isSameFormat(String ipStart, String ipEnd) {
		return (
				(IPAddressUtil.isIPv4LiteralAddress(ipStart)) && 
				(IPAddressUtil.isIPv4LiteralAddress(ipEnd))
			   ) || 
			   (
				(IPAddressUtil.isIPv6LiteralAddress(ipStart)) && 
				(IPAddressUtil.isIPv6LiteralAddress(ipEnd))
			   );
	}
}