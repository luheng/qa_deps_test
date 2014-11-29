package scorer;

import data.DepSentence;

public class PunctuationScorer {

	/*
	 * Simply "wipe-out" all scores for punctuation.
	 */
	public void processScores(double[][] scores, DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			if (sentence.getPostagString(i).equals(".")) {
				for (int j = 0; j < sentence.length; j++) {
					scores[i][j] = scores[j][i] = 0.0;
				}
			}
		}
	}
}
