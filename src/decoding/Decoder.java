package decoding;

public interface Decoder {
	// Return max score.
	public double decode(double[][] scores, int[] parents);
}
