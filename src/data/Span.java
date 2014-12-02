package data;

/**
 * Follow the convention, span(a,b) means everything from a to b-1.
 * @author luheng
 *
 */
public class Span {
	public int left, right;
	
	public Span(int left, int right) {
		this.left = left;
		this.right = right;
	}
	
	public int size() {
		return right - left;
	}
}
