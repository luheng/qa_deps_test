package annotation;

import data.DepSentence;

public class WordRank {
	public DepSentence sentence;
	public int wordID; // If there are multiple words, use the ID of the head.
	public int[] wordSpan;
	public double score;
	
	public WordRank(DepSentence sentence, int wordID) {
		this.sentence = sentence;
		this.wordID = wordID;
		this.wordSpan = new int[] {wordID, wordID + 1};
		this.score = 0.0;
	}
	
	public WordRank(DepSentence sentence, int headWordID, int[] wordSpan) {
		this.sentence = sentence;
		this.wordID = headWordID;
		this.wordSpan = new int[] {wordSpan[0], wordSpan[1]};
		this.score = 0.0;
	}
	
	public String toString() {
		return String.format("<%s,%s,%.2f>", sentence.getTokenString(wordSpan),
				sentence.getPostagString(wordID), score);
	}
}
