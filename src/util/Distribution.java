package util;

import java.util.ArrayList;

// TODO: print histogram and blah blah.

public class Distribution {	
	ArrayList<Double> values;
	double sum, minValue, maxValue;
	
	public Distribution() {
		values = new ArrayList<Double>();
		sum = .0;
	}
	
	public void clear() {
		values.clear();
		sum = 0;
	}
	
	public void add(double val) {
		minValue = values.isEmpty() ? val : Math.min(minValue, val);
		maxValue = values.isEmpty() ? val : Math.max(maxValue, val);
		sum += val;
		values.add(val);
	}
	
	public double getSum() {
		return sum; 
	}
	
	public double getMean() {
		return sum / values.size();
	}
	
	public double getStd() {
		
	}
	
	public double getMin() {
		return minValue;
	}
	
	public double getMax() {
		return maxValue;
	}
	
	public int getNumSamples() {
		return values.size();
	}
}
