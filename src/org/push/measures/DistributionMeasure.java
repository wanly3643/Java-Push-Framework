package org.push.measures;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Lei Wang
 */

public class DistributionMeasure extends Measure {

	private Map<String, Double> mappedValues =
		new HashMap<String, Double>();

	public DistributionMeasure(String name) {
		super(name);
	}

    @Override
	public void addObservation(MeasureArgs args) {
	    if (!(args instanceof DistributionMeasureArgs)) {
	    	return;
        }

	    DistributionMeasureArgs myArgs = (DistributionMeasureArgs) args;

	    Double d = mappedValues.get(myArgs.serviceName);
	    if (d == null) {
	    	mappedValues.put(myArgs.serviceName, new Double(myArgs.value));
	    } else {
	    	mappedValues.put(myArgs.serviceName, 
                    new Double(d.doubleValue() + myArgs.value));
	    }
	}


    @Override
	public String collectAndReset(String timeStamp) {
	    //Collect
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(name).append(">");
		for (Map.Entry<String, Double> entry :mappedValues.entrySet())
		{
			sb.append("<").append(entry.getKey());
			sb.append(" val=\"").append(entry.getValue()).append("\"/>");
		}
		sb.append("</").append(name).append(">");

	    //Reset
	    mappedValues.clear();

	    return sb.toString();
	}
}
