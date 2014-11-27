  	package annotation;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

public class GreedyQuestionAnswerAligner implements QuestionAnswerAligner {

	@Override
	public void align(DepSentence sentence, QAPair qa) {
		String[] sentenceTokens = sentence.corpus.wordDict.getStringArray(
				sentence.tokens);
		greedyMatch(qa.questionAlignment, sentenceTokens, qa.questionTokens);
		greedyMatch(qa.answerAlignment, sentenceTokens, qa.answerTokens);
	}
	
	// Always prefer longest spans.
	private void greedyMatch(int[] alignment, String[] sentenceTokens,
							 String[] qaTokens) {
		boolean[] matched = new boolean[sentenceTokens.length];
		Arrays.fill(alignment, -1);
		Arrays.fill(matched, false);
		for (int i = 0; i < qaTokens.length; i++) {
			if (alignment[i] != -1) {
				continue;
			}
			int best = 0, bestMatch = -1;
			// Choose start point of the sentence.
			for (int j = 0; j + best < sentenceTokens.length; j++) {
				int k = 0;
				for ( ; j + k < sentenceTokens.length &&
						i + k < qaTokens.length; k++) {
					// This is a special treatment: Sometimes the answer ends with a
					// single period that has nothing to do with the original sentence.
					// We discard that punctuation for now.
					if (i+k == qaTokens.length - 1 && qaTokens[i+k].equals(".")) {
						break;
					}
					if (!qaTokens[i+k].equalsIgnoreCase(sentenceTokens[j+k]) ||
						matched[j+k]) {
						break;
					}
				}
				if (k > best) {
					best = k;
					bestMatch = j;
				}
			}
 			if (best > 0) {
				// System.out.println(String.format("%d, %d, %d", 
				// 		i, bestMatch, best));
				for (int k = 0; k < best; k++) {
					alignment[i+k] = bestMatch + k;
					matched[bestMatch+k] = true;
				}
			}
		}
	}

}
