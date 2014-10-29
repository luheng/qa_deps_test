package decoding;

import java.util.Arrays;

/*
 * Used to decode for projective dependency trees in arc-factored model.
 * 
 * From "Reestimation and Best-First Parsing Algorithm for Probabilistic Dependency Grammars"
 * Lee and Choi, 1997
 */
public class ViterbiDecoder implements Decoder {

	public void decode(AdjacencyGraph graph, int[] parents) {
		// The graph contains all the tokens plus the root.
		int length = graph.numNodes;
		double[][] linkScore = new double[length][length];
		double[][] seqScore= new double[length][length];
		int[][] linkBest = new int[length][length];
		int[][] seqBest = new int[length][length]; 
		
		for(int i = 0; i < length; i++) {
			seqScore[i][i] = 0; 
			linkScore[i][i] = Double.NEGATIVE_INFINITY;
		}

		for(int i = 0; i < length - 1; i++) {
			seqScore[i][i+1] = linkScore[i][i+1] = graph.edges[i+1][i]; 
			seqScore[i+1][i] = linkScore[i+1][i] = graph.edges[i][i+1]; 
			
			for(int j = i + 2; j < length; j++) {
				linkScore[i][j] = linkScore[j][i] = Double.NEGATIVE_INFINITY;
				seqScore[i][j] = seqScore[j][i] = Double.NEGATIVE_INFINITY;
			}
		}
		
		for(int k = 2; k < length; k++) {
			// Tree can only have one root.
			linkScore[0][k] = graph.edges[0][k] + seqScore[k][1];
			linkBest[0][k] = 1;
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
					double mScore = graph.edges[i][j] + seqScore[i][m] +
									seqScore[j][m+1];
					if (mScore > linkScore[i][j]) {
						linkScore[i][j] = mScore;
						linkBest[i][j] = m;
					}
					// Leftward link.
					mScore = graph.edges[j][i] + seqScore[i][m] +
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
		backtrack(0, 0, length - 1, parents, seqBest, linkBest);
	}
	
	// Type: 0 for sequence, 1 for link
	private void backtrack(int type, int start, int end, int[] parents,
						   int[][] seqBest, int[][] linkBest) {
		if (start == end) {
			return;
		}
		if (end == start + 1 || end == start - 1) {
			parents[end] = start;
			return;
		}
		if (type == 0) {
			int best = seqBest[start][end];
			backtrack(0, start, best, parents, seqBest, linkBest);
			backtrack(1, best, end, parents, seqBest, linkBest);
		} else {
			parents[end] = start;
			int best = linkBest[start][end];
			backtrack(0, start, best, parents, seqBest, linkBest);
			backtrack(0, end, best + 1, parents, seqBest, linkBest);
		}
	}
	
	// Test decoder.
	public static void main(String[] args) {
		AdjacencyGraph graph = AdjacencyGraph.getDistanceWeightedGraph(5);
		int[] parents = new int[graph.numNodes];
		Arrays.fill(parents, 0);
		
		ViterbiDecoder decoder = new ViterbiDecoder();
		decoder.decode(graph, parents);
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
