package org.push.measures;

/**
 * 
 * @author Lei Wang
 */

public class CumulMeasure extends Measure {

	private boolean blnIsContinous;
	private double cumulValue;

	public CumulMeasure(String name) {
		this(name, false);
	}

	public CumulMeasure(String name, boolean bIsContinous) {
		super(name);
	    this.blnIsContinous = bIsContinous;
	    this.cumulValue = 0;
	}

    @Override
	public void addObservation(MeasureArgs args) {
	    if (!(args instanceof CumulMeasureArgs)) {
	    	return;
        }

	    CumulMeasureArgs myArgs = (CumulMeasureArgs) args;
	    cumulValue = cumulValue + myArgs.value;
	}

    @Override
	public String collectAndReset(String timeStamp) {
	    //Collect
		StringBuilder sb = new StringBuilder();

		sb.append("<").append(name).append(" val=\"").append(cumulValue).append("\"/>");

	    //Reset to zero if not continuous.
	    if (!blnIsContinous) {
	        cumulValue = 0;
        }
	    //
	    return sb.toString();
	}
}
