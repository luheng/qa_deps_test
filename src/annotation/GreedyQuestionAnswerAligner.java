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
	
	private void alignQuestion(String[] sentenceTokens, String[] questionTokens,
			int[] alignment) {
		for (int i = 0; i < questionTokens.length; i++) {
			for (int j = 0; j < sentenceTokens.length; j++) {
				if (questionTokens[i].equalsIgnoreCase(sentenceTokens[j])) {
					alignment[i] = j;
					break;
				}
			}
		}
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
			int best = 0, bestMatch = 0;
			for (int j = 0; j + best < sentenceTokens.length; j++) {
				int k = 0;
				for ( ; j + k < sentenceTokens.length &&
						i + k < qaTokens.length; k++) {
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
