package evaluation;

import java.util.ArrayList;

public class Results {
	public ArrayList<Double> results;

	public Results() {
		results = new ArrayList<Double>();
	}
	
	public void add(double r) {
		results.add(r);
	}
	
	public int size() {
		return results.size();
	}
	
	public double average() {
		double avg = .0;
		for (double r : results) {
			avg += r;
		}
		return avg / size();
	}
	
	public double std() {
		double avg = average();
		double std = .0;
		for (double r : results) {
			std += (r - avg) * (r - avg);
		}
		return Math.sqrt(std / size());
	}
}
