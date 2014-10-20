	package annotation;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

public class GreedyQuestionAnswerAligner implements QuestionAnswerAligner {

	@Override
	public void align(DepSentence sentence, QAPair qa) {
		String[] sentenceTokens = sentence.corpus.wordDict.getStringArray(
				sentence.tokens);
		//alignQuestion(sentenceTokens, qa.questionTokens, qa.questionAlignment);
		greedyMatch(sentenceTokens, qa.questionTokens, qa.questionAlignment);
		greedyMatch(sentenceTokens, qa.answerTokens, qa.answerAlignment);
	}
	
	// Always prefer longest spans.
	private void greedyMatch(String[] sentenceTokens, String[] qaTokens,
							 int alignment[]) {
		boolean[] matched = new boolean[sentenceTokens.length];
		Arrays.fill(alignment, -1);
		Arrays.fill(matched, false);
		for (int i = 0; i < qaTokens.length; i++) {
			if (alignment[i] != -1) {
				continue;
			}
			// This is a special treatment: Sometimes the answer ends with a
			// single period that has nothing to do with the original sentence.
			// We discard that punctuation for now.
			if (i == qaTokens.length - 1 && qaTokens[i].equals(".")) {
				break;
			}
			int best = 0, bestMatch = 0;
			for (int j = 0; j + best < sentenceTokens.length; j++) {
				int k = 0;
				for ( ; j + k < sentenceTokens.length &&
						i + k < qaTokens.length; k++) {
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
				alignment[i] = bestMatch;
				for (int j = 0; j < best; j++) {
					alignment[i+j] = bestMatch + j;
					matched[bestMatch+j] = true;
				}
			}
		}
	}

}
