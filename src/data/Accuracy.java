package data;

public class Accuracy {
	public int numMatched, numCounted;
	public Accuracy(int numMatched, int numCounted) {
		this.numMatched = numMatched;
		this.numCounted = numCounted;
	}
	
	public double accuracy() {
		return 1.0 * numMatched / numCounted;
	}
	
	public void add(Accuracy acc) {
		this.numMatched += acc.numMatched;
		this.numCounted += acc.numCounted;
	}
}