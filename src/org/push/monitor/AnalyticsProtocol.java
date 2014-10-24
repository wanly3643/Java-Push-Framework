package org.push.monitor;

import org.push.exception.NoSuchEnumElementException;
import org.push.util.CppEnum;

/**
 * 
 * @author Lei Wang
 */

public enum AnalyticsProtocol implements CppEnum {

	LoginRequest(1),
    LoginResponse(2),

    ConsoleCommandRequest(3),
    ConsoleCommandResponse(4),

    LiveSubscriptionRequest(5),
    InitializationResponse(6),
    MeasuresResponse(7),
    VisitorInResponse(8),
    VisitorOutResponse(9),


    SessionsRequest(10),
    SessionsResponse(11),
    StatsRequest(12),
    StatsResponseBegin(13),
    StatsResponse(14),
    StatsResponseEnd(15),
    GeoStatsRequest(16),
    GeoStatsResponse(17),

    LogoutRequest(18);

    private int value;

    private AnalyticsProtocol(int value) { this.value = value; }

    public int value() { return this.value; }
    
    public static AnalyticsProtocol get(int value) 
    	throws NoSuchEnumElementException
    {
    	int idx = value - 1;
    	
    	AnalyticsProtocol[] vals = AnalyticsProtocol.values();
    	
    	if (idx < 0 || idx >= vals.length) {
    		throw new NoSuchEnumElementException(
    				"No element of AnalyticsProtocol with value: " + value);
    	}
    	
    	return vals[idx];
    }
}
