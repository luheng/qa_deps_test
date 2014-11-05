package decoding;

import java.util.Arrays;

/*
 * Used to decode for projective dependency trees in arc-factored model.
 * 
 * From "Reestimation and Best-First Parsing Algorithm for Probabilistic Dependency Grammars"
 * Lee and Choi, 1997
 */
public class ViterbiDecoder implements Decoder {

	public void decode(double[][] scores, int[] parents) {
		// The graph contains all the tokens plus the root.
		int length = scores.length;
		double[][] linkScore = new double[length][length];
		double[][] seqScore= new double[length][length];
		int[][] linkBest = new int[length][length];
		int[][] seqBest = new int[length][length]; 
		
		for(int i = 0; i < length; i++) {
			seqScore[i][i] = 0; 
			linkScore[i][i] = Double.NEGATIVE_INFINITY;
		}
		// Initialize all scores.
		for(int i = 0; i < length - 1; i++) {
			seqScore[i][i+1] = linkScore[i][i+1] = scores[i][i+1]; 
			seqScore[i+1][i] = linkScore[i+1][i] = scores[i+1][i]; 
			for(int j = i + 2; j < length; j++) {
				linkScore[i][j] = linkScore[j][i] = Double.NEGATIVE_INFINITY;
				seqScore[i][j] = seqScore[j][i] = Double.NEGATIVE_INFINITY;
			}
		}
		// Enumerate all span lengths.
		for(int k = 2; k < length; k++) {
			// All dependency tree can only have one root.
			linkScore[0][k] = scores[0][k] + seqScore[k][1];
			linkBest[0][k] = 0;
			for(int m = 0; m < k; m++) {
				double mScore = seqScore[0][m] + linkScore[m][k];
				if (mScore > seqScore[0][k]) {
					seqScore[0][k] = mScore;
					seqBest[0][k] = m;
				}
			}
			for(int i = 1; i < length - k; i++) {
				int j = i + k;
				for(int m = i; m < j; m++) {
					// Rightward link. 
					double mScore = scores[i][j] + seqScore[i][m] +
									seqScore[j][m+1];
					if (mScore > linkScore[i][j]) {
						linkScore[i][j] = mScore;
						linkBest[i][j] = m;
					}
					// Leftward link.
					mScore = scores[j][i] + seqScore[i][m] +
							 seqScore[j][m+1];
					if (mScore > linkScore[j][i]) {
						linkScore[j][i] = mScore;
						linkBest[j][i] = m;
					}
				}
				// Rightward sequence.
				for(int m = i; m < j; m++) {
					double mScore = seqScore[i][m] + linkScore[m][j];
					if (mScore > seqScore[i][j]) {
						seqScore[i][j] = mScore;
						seqBest[i][j] = m;
					}
				}
				// Leftward sequence.
				for(int m = i + 1; m <= j; m++) {
					double mScore = seqScore[j][m] + linkScore[m][i];
					if (mScore > seqScore[j][i]) {
						seqScore[j][i] = mScore;
						seqBest[j][i] = m;
					}
				}
			}
		}
	
		// Resolve parents
		assert (parents.length == length - 1);
		backtrack(0, 0, length - 1, parents, seqBest, linkBest);
	}
	
	// Type: 0 for sequence, 1 for link
	private void backtrack(int type, int start, int end, int[] parents,
						   int[][] seqBest, int[][] linkBest) {
		/* Debugging info.
		System.out.println((type == 0 ? "seq" : "link") + "\t" + start + "\t" +
							end);
		for (int i = 0; i < parents.length; i++) {
			System.out.print(parents[i] + "\t");
		}
		System.out.println();
		*/
		if (start == end) {
			return;
		}
		
		if (end == start + 1 || end == start - 1) {
			/* Debugging info.
			System.out.println("here:\t" + start + ", " + end);
			*/
			parents[end - 1] = start - 1;
			return;
		}
		if (type == 0) {
			int best = seqBest[start][end];
			backtrack(0, start, best, parents, seqBest, linkBest);
			backtrack(1, best, end, parents, seqBest, linkBest);
		} else {
			parents[end - 1] = start - 1;
			int best = linkBest[start][end];
			if (start < end) {
				backtrack(0, start, best, parents, seqBest, linkBest);
				backtrack(0, end, best + 1, parents, seqBest, linkBest);
			} else {
				backtrack(0, start, best + 1, parents, seqBest, linkBest);
				backtrack(0, end, best, parents, seqBest, linkBest);
			}
		}
	}
	
	// Test decoder.
	public static void main(String[] args) {
		AdjacencyGraph graph = AdjacencyGraph.getDistanceWeightedGraph(5);
		int[] parents = new int[graph.numNodes - 1];
		Arrays.fill(parents, 0);
		
		ViterbiDecoder decoder = new ViterbiDecoder();
		decoder.decode(graph.edges, parents);
		for (int i = 0; i < parents.length; i++) {
			System.out.print(i + "\t");
		}
		System.out.println();
		for (int i = 0; i < parents.length; i++) {
			System.out.print(parents[i] + "\t");
		}
		System.out.println();
	}
}
