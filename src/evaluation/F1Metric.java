package evaluation;

public class F1Metric {

	public int numMatched, numGold, numProposed;
	
	public F1Metric() {
		this.numMatched = this.numGold = this.numProposed = 0;
	}
	
	public F1Metric(int numMatched, int numGold, int numProposed) {
		this.numMatched = numMatched;
		this.numGold = numGold;
		this.numProposed = numProposed;
	}
	
	public double precision() {
		return numProposed == 0 ? 0 : 1.0 * numMatched / numProposed;
	}
	
	public double recall() {
		return numGold == 0 ? 0 : 1.0 * numMatched / numGold;
	}
	
	public double f1() {
		double precision = precision(),
			   recall = recall();
		return precision + recall == 0 ?
				0 : 2 * precision * recall / (precision + recall); 
	}
}
