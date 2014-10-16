package annotation;

public class MatchedSpan {
	public int idx1, idx2, length;
	
	public MatchedSpan(int idx1, int idx2, int length) {
		this.idx1 = idx1;
		this.idx2 = idx2;
		this.length = length;
	}
}
