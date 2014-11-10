package scorer;

import util.LatticeUtils;
import data.DepSentence;

/**
 * Scoring based on distance - favoring short arcs.
 * @author luheng
 *
 */
public class DistanceScorer {

	final double minScore = 1e-5;
	final double leftBranchingBias = 0.1;
	
	public void getScores(double[][] scores, DepSentence sentence) {
		assert (sentence.length + 1 == scores.length);
		int length = sentence.length;
		LatticeUtils.fill(scores, 0.0);
		for (int i = 1; i <= length; i++) {
			for (int j = 1; j <= length; j++) {
				int dist = Math.abs(i - j);
				if (i < j) {
					// Right branching case.
					scores[i][j] = Math.max(minScore, 1.0 / dist);
				} else if (i == j + 1) {
					// Left branching case.
					scores[i][j] =
							Math.max(minScore, 1.0 / dist + leftBranchingBias);
				}
			}
		}
	}
}
