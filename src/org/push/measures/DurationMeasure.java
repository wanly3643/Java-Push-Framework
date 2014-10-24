package org.push.measures;

import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Lei Wang
 */

public class DurationMeasure extends Measure {

	private List<Double> durationList = new LinkedList<Double> ();
	//
	private double getMean() {
	    double total = 0;
	    int count = 0;

	    for (Double d : durationList) {
	    	total += d.doubleValue();
	        count ++;
	    }
	    return count == 0 ? 0 : (total / (double)count);
	}

	private double getDispersion(double mean) {
	    double temp= 0;
	    int count = 0;
	    for (Double d : durationList)
	    {
	        temp += Math.pow( d.doubleValue() - mean,2);
	        count ++;
	    }
	    return count == 0 ? 0 : Math.sqrt(temp / (double)count);
	}

	public DurationMeasure(String name) {
		super(name);
	}

    @Override
	public void addObservation(MeasureArgs args) {
		if (!(args instanceof DurationMeasureArgs)) {
            return;
        }

	    DurationMeasureArgs myArgs = (DurationMeasureArgs) args;
	    durationList.add(new Double(myArgs.duration));
	}


    @Override
	public String collectAndReset(String timeStamp) {
	    //Calculate Average and dispersion
	    double mean = getMean();
	    double dispersion = getDispersion(mean);

	    StringBuilder sb = new StringBuilder();
	    sb.append("<").append(name).append(" mean=\"")
          .append(mean).append("\" disp=\"")
          .append(dispersion).append("\" />");

	    //Reset
	    durationList.clear();

	    return sb.toString();
	}

}
