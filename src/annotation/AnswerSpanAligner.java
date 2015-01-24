package annotation;

import java.util.ArrayList;
import java.util.Arrays;

import data.DepSentence;

public class AnswerSpanAligner {
	public static int[][] align(DepSentence sentence, String answer) {
		String[] ansTokens = answer.split("\\s+");
		String[] sentTokens = sentence.corpus.wordDict.getStringArray(
				sentence.tokens);
		
		boolean[] matched = new boolean[sentTokens.length];
		Arrays.fill(matched, false);
		ArrayList<int[]> spans = new ArrayList<int[]>();
		
		for (int i = 0; i < ansTokens.length; i++) {
			int maxLength = 0, bestMatch = -1;
			for (int j = 0; j + maxLength < sentTokens.length; j++) {
				int k = 0;
				for ( ; j + k < sentTokens.length &&
						i + k < ansTokens.length; k++) {
					if (!ansTokens[i+k].equalsIgnoreCase(sentTokens[j+k]) ||
						matched[j+k]) {
						break;
					}
				} 
				if (k > maxLength) {
					maxLength = k;
					bestMatch = j;
				}
			}
 			if (maxLength > 0) {
 				spans.add(new int[] {bestMatch, bestMatch + maxLength});
				for (int k = 0; k < maxLength; k++) {
					matched[bestMatch+k] = true;
				}
				i += maxLength - 1;
			}
		}
		int[][] results = new int[spans.size()][];
		for (int i = 0; i < spans.size(); i++) {
			results[i] = new int[] {spans.get(i)[0], spans.get(i)[1]};
		}
		return results;
	}
}
