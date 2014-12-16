package annotation;

import java.util.Comparator;

import data.DepSentence;

/**
 * A word in a sentence to ask questions about.
 * i.e. "concedes" can be asked with question "Who concedes?".
 * @author luheng
 *
 */
public class CandidateProposition {
	public DepSentence sentence;
	public int wordID; // If there are multiple words, use the ID of the head.
	public int[] wordSpan, effectiveSpan;
	public double score;
	
	public CandidateProposition(DepSentence sentence, int wordID) {
		this.sentence = sentence;
		this.wordID = wordID;
		this.wordSpan = new int[] {wordID, wordID + 1};
		this.effectiveSpan = new int[] {0, sentence.length };
		this.score = 0.0;
	}
	
	public CandidateProposition(DepSentence sentence, int headWordID,
								int[] wordSpan) {
		this.sentence = sentence;
		this.wordID = headWordID;
		this.wordSpan = new int[] {wordSpan[0], wordSpan[1]};
		this.effectiveSpan = new int[] {0, sentence.length };
		this.score = 0.0;
	}
	
	public String getPropositionString() {
		return sentence.getTokenString(wordSpan);
	}
	
	public String toString() {
		return String.format("<%s,%s,%.2f,%d-%d>",
				sentence.getTokenString(wordSpan),
				sentence.getPostagString(wordID),
				score,
				effectiveSpan[0], effectiveSpan[1]);
	}
	
	public static Comparator<CandidateProposition> comparator =
			new Comparator<CandidateProposition>() {
		public int compare(CandidateProposition w1, CandidateProposition w2) {
			if (w1.score > w2.score + 1e-8) {
				return -1;
			} else if (w1.score + 1e-8 < w2.score) {
				return 1;
			}
			int l1 = w1.effectiveSpan[1] - w1.effectiveSpan[0];
			int l2 = w2.effectiveSpan[1] - w2.effectiveSpan[0];
			return l1 - l2;
		}
	};
}
