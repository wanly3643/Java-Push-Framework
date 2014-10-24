package org.push.measures;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Lei Wang
 */

public class KeyedAveragedMeasure extends Measure {

	private Map<Integer, Double> mappedValues =
		new HashMap<Integer, Double>();

	public KeyedAveragedMeasure(String name) {
		super(name);
	}

    @Override
	public void addObservation(MeasureArgs args) {
	    if (!(args instanceof MKAveragedMeasureArgs)) {
            return;
        }

		MKAveragedMeasureArgs myArgs = (MKAveragedMeasureArgs) args;
	    Integer key = new Integer(myArgs.key);
	    Double d = mappedValues.get(key);
	    if (d == null) {
	    	mappedValues.put(key, new Double(myArgs.value));
	    } else {
	    	mappedValues.put(key, new Double(d.doubleValue() + myArgs.value));
	    }
	}

	private double getMean() {
	    double total = 0;
	    int count = 0;
	    
	    for (Double d : mappedValues.values()) {
	    	total += d.doubleValue();
	    	count ++;
	    }
	    return count == 0 ? 0 : (total / (double)count);
	}

	private double getDispersion(double mean) {
	    double temp = 0;
	    int count = 0;

	    for (Double d : mappedValues.values())
	    {
	    	temp += Math.pow(d.doubleValue() - mean, 2);
	    	count ++;
	    }
	    return count == 0 ? 0 : Math.sqrt(temp / (double)count);
	}


    @Override
	public String collectAndReset(String timeStamp) {
	    //Calculate average and dispersion
	    double mean = getMean();
	    double dispersion = getDispersion(mean);

	    StringBuilder sb = new StringBuilder();

	    sb.append("<").append(name).append(" mean=\"").append(mean);
	    sb.append("\" disp=\"").append(dispersion).append("\" />");

	    //Reset
	    mappedValues.clear();

	    return sb.toString();
	}

}
