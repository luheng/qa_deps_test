package annotation;

import data.DepSentence;


public class QuestionTemplate {

	String postag, token, whWord;
	String[] questionTokens;
	int argNum; // 0 or 1.
	int slotID;
	boolean requireLeftTokens, requireRightTokens;
	
	public QuestionTemplate(String postag, String token, String whWord,
							int argNum, String question,
							boolean requireLeftTokens,
							boolean requireRightTokens) {
		this.postag = postag;
		this.token = token;
		this.whWord = whWord;
		this.argNum = argNum;
		this.questionTokens = question.split("\\s+");
		this.slotID = -1;
		for (int i = 0; i < questionTokens.length; i++) {
			if (questionTokens[i].equals("###")) {
				slotID = i;
			}
		}
		this.requireLeftTokens = requireLeftTokens;
		this.requireRightTokens = requireRightTokens;
	}

	/*
	public boolean matches(DepSentence sentence, int wordID) {
		return (this.postag.isEmpty() ||
				sentence.getPostagString(wordID).equalsIgnoreCase(
						this.postag)) &&
			   (this.token.isEmpty() ||
			    sentence.getTokenString(wordID).equalsIgnoreCase(this.token));
	}
	*/
	
	public boolean matches(DepSentence sentence, QuestionWord word, int[] span) {
		int wordID = word.wordID;
		return (this.postag.isEmpty() ||
				sentence.getPostagString(wordID).equalsIgnoreCase(
						this.postag)) &&
			   (this.token.isEmpty() ||
				sentence.getTokenString(wordID).equalsIgnoreCase(this.token)) &&
			   (!this.requireLeftTokens || word.wordSpan[0] > span[0]) &&
			   (!this.requireRightTokens || word.wordSpan[1] + 1 < span[1]);
	}
	
	public String getQuestionString(DepSentence sentence, QuestionWord word) {
		String wordStr = sentence.getTokenString(word.wordSpan);
		String qstr = "";
		for (int i = 0; i < questionTokens.length; i++) {
			if (i > 0) {
				qstr += " ";
			}
			qstr += (i == slotID ? wordStr : questionTokens[i]);
		}
		return qstr;
	}
	
	public String getNumberedQuestionString(DepSentence sentence,
			QuestionWord word) {
		String wordStr = "";
		for (int i = word.wordSpan[0]; i < word.wordSpan[1]; i++) {
			wordStr += String.format("%s(%d) ", sentence.getTokenString(i), i);
		}
		wordStr = wordStr.trim();
		String qstr = "";
		for (int i = 0; i < questionTokens.length; i++) {
			if (i > 0) {
				qstr += " ";
			}
			qstr += (i == slotID ? wordStr : questionTokens[i]);
		}
		return qstr;
	}
	
	public int getSlotID() {
		return slotID;
	}
}
	

