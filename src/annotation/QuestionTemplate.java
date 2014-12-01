package annotation;

import data.DepSentence;


public class QuestionTemplate {

	String postag, token, whWord, question;
	int argNum; // 0 or 1.
	
	public QuestionTemplate(String postag, String token, String whWord,
							int argNum, String question) {
		this.postag = postag;
		this.token = token;
		this.whWord = whWord;
		this.argNum = argNum;
		this.question = question;
	}

	public boolean matches(DepSentence sentence, int wordID) {
		return (this.postag.isEmpty() ||
				sentence.getPostagString(wordID).equalsIgnoreCase(
						this.postag)) &&
			   (this.token.isEmpty() ||
			    sentence.getTokenString(wordID).equalsIgnoreCase(this.token));
	}
	
	public String getQuestion(DepSentence sentence, int wordID) {
		String word = sentence.getTokenString(wordID);
		return this.question.replace("###", word.toLowerCase());
	}
	
	public String getNumberedQuestion(DepSentence sentence, int wordID) {
		String word = sentence.getTokenString(wordID);
		return this.question.replace("###",
				String.format("%s(%d)", word.toLowerCase(), wordID));
	}
}
	

