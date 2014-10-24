package org.push.measures;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Lei Wang
 */

public class KeyedAveragedDistributionMeasure extends Measure {

	private Map<String, Map<String, Double>> mappedValues =
		new HashMap<String, Map<String, Double>>();

    public KeyedAveragedDistributionMeasure(String name) {
    	super(name);
    }

    private void addInnerObservation(Map<String, Double> segment, 
            String key, double value) {
    	Double d = segment.get(key);
    	if (d == null) {
    		segment.put(key, new Double(value));
    	} else {
    		segment.put(key, new Double(d.doubleValue() + value));
    	}
    }

    private double getMean(Map<String, Double> segment) {
	    double total = 0;
	    int count = 0;
	    
	    for (Double d : segment.values()) {
	    	total += d.doubleValue();
	    	count ++;
	    }
	    return count == 0 ? 0 : (total / (double)count);
	}

    private double getDispersion(Map<String, Double> segment, double mean ) {
	    double temp = 0;
	    int count = 0;
	    
	    for (Double d : segment.values()) {
	    	temp += Math.pow(d.doubleValue() - mean, 2);
	    	count ++;
	    }
	    return count == 0 ? 0 : Math.sqrt(temp / (double)count);
    }

    @Override
	public void addObservation(MeasureArgs args) {
		if (!(args instanceof KeyedAveragedDistributionMeasureArgs)) {
            return;
        }

	    KeyedAveragedDistributionMeasureArgs myArgs = 
                (KeyedAveragedDistributionMeasureArgs) args;

	    Map<String, Double> innerObservation = 
                mappedValues.get(myArgs.serviceName);
	    if (innerObservation == null) {
	    	innerObservation = new HashMap<String, Double>();
	    	mappedValues.put(myArgs.serviceName, innerObservation);
	    	
	    }

	    addInnerObservation(innerObservation, myArgs.key, myArgs.value);
	}

    @Override
	public String collectAndReset(String timeStamp) {
	    //Collect
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(name).append(">");

		double mean;
		double dispersion;
		for (Map.Entry<String, Map<String, Double>> entry : 
                mappedValues.entrySet()) {
			mean = getMean(entry.getValue());
			dispersion = getDispersion(entry.getValue(), mean);
			sb.append("<").append(entry.getKey());
			sb.append(" mean=\"").append(mean);
			sb.append("\" disp=\"").append(dispersion).append("\"/>");
		}
		sb.append("</").append(name).append(">");
		
		// Reset
		for (Map<String, Double> innerObservation : mappedValues.values()) {
			innerObservation.clear();
		}
		mappedValues.clear();

	    return sb.toString();
	}

}
