package annotation;

import java.util.Arrays;

import util.StringUtils;
import data.DepSentence;
import data.QAPair;

public class DistanceSensitiveQuestionAnswerAligner
		implements QuestionAnswerAligner {

	@Override
	public void align(DepSentence sentence, QAPair qa) {
		String[] sentenceTokens = sentence.corpus.wordDict.getStringArray(
				sentence.tokens);
		greedyMatch(qa.answerAlignment, sentenceTokens, qa.answerTokens, null);
		int[] answerSpan = new int[2];
		answerSpan[0] = qa.answerAlignment[0];
		answerSpan[1] = qa.answerAlignment[qa.answerAlignment.length - 1];
		if (answerSpan[1] == -1) {
			answerSpan[1] = qa.answerAlignment[qa.answerAlignment.length - 2];
		}
		//System.out.println(answerSpan[0] + ", " + answerSpan[1] + ", " +
		//				   StringUtils.join(" ", qa.answerTokens));
		greedyMatch(qa.questionAlignment, sentenceTokens, qa.questionTokens,
				    answerSpan);
	}
	
	/**
	 * Preferring longest span. If there are multiple spans with the same
	 * length, preferring those closest to the reference span (i.e. matched
	 * answer span).
	 * @param alignment
	 * @param sentenceTokens
	 * @param qaTokens
	 * @param referenceSpan
	 */
	private void greedyMatch(int[] alignment, String[] sentenceTokens,
							 String[] qaTokens, int[] referenceSpan) {
		boolean[] matched = new boolean[sentenceTokens.length];
		Arrays.fill(alignment, -1);
		Arrays.fill(matched, false);
		for (int i = 0; i < qaTokens.length; i++) {
			if (alignment[i] != -1) {
				continue;
			}
			int maxLength = 0, minDistance = Integer.MAX_VALUE, bestMatch = -1;
			// Choose start point of the sentence.
			for (int j = 0; j + maxLength < sentenceTokens.length; j++) {
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
				// Yes, the question word can be inside the answer span. But we
				// are not considering the case here.
				int dist = computeSpanDistance(new int[] {j, j + k},
											   referenceSpan); 
				if (k > maxLength) {
					maxLength = k;
					minDistance = dist;
					bestMatch = j;
					
				} else if (k == maxLength && dist < minDistance) {
					//System.out.println(k + ", " + dist + ", " + minDistance +
					//				   ", " + qaTokens[i]);
					minDistance = dist;
					bestMatch  = j;
				}
			}
 			if (maxLength > 0) {
				// System.out.println(String.format("%d, %d, %d", 
				// 		i, bestMatch, best));
				for (int k = 0; k < maxLength; k++) {
					alignment[i+k] = bestMatch + k;
					matched[bestMatch+k] = true;
				}
			}
		}
	}
	
	private int computeSpanDistance(int[] span, int[] refSpan) {
		if (refSpan == null) {
			return 0;
		}
		return Math.min(Math.min(Math.abs(span[0] - refSpan[0]),
								 Math.abs(span[0] - refSpan[1])),
						Math.min(Math.abs(span[1] - refSpan[0]),
								 Math.abs(span[1] - refSpan[1])));
	}

}
