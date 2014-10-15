package data;

import util.StringUtils;

public class QAPair {
	String[] questionTokens, answerTokens;
	
	public QAPair(String question, String answer) {
		questionTokens = question.split("\\s+");
		answerTokens = answer.split("\\s+");
	}
	
	@Override
	public String toString() {
		return StringUtils.join(" ", questionTokens) + "\t" +
			   StringUtils.join(" ", answerTokens);
	}
	
}
