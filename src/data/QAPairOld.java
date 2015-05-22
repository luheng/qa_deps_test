package data;

import java.util.Arrays;

import util.StrUtils;

public class QAPairOld {
	public String[] questionTokens, answerTokens, propositionTokens;
	// Contains indices of words in the original sentence. -1 means unaligned.
	public int[] questionAlignment, answerAlignment, propositionAlignment;
	
	// FIXME: try to come up with a better name.
	// If the answer is an "alternative answer" of another, then it has this
	// "mainQA" field set.
	// i.e. in the sentence "The robot moved because it is programmed to do so".
	// and the question "What is programmed to do something?", 
	// the main answer would be "it", and an alternative one is "the robot".
	// Usually we can figure this out by distance in the original sentence.
	public QAPairOld mainQA;
	
	public QAPairOld(String question, String answer) {
		questionTokens = question.split("\\s+");
		answerTokens = answer.split("\\s+");
		for (int i = 0; i < questionTokens.length; i++) {
			questionTokens[i] = questionTokens[i].trim();
		}
		for (int i = 0; i < answerTokens.length; i++) {
			answerTokens[i] = answerTokens[i].trim();
		}
		propositionTokens = null;
		
		questionAlignment = new int[questionTokens.length];
		answerAlignment = new int[answerTokens.length];
		Arrays.fill(questionAlignment, -1);
		Arrays.fill(answerAlignment, -1);
		
		mainQA = null;
	}
	
	public QAPairOld(String question, String answer, String proposition) {
		this(question, answer);
		this.propositionTokens = proposition.split("\\s+");
		for (int i = 0; i < propositionTokens.length; i++) {
			propositionTokens[i] = propositionTokens[i].trim();
		}
		propositionAlignment = new int[propositionTokens.length];
		Arrays.fill(propositionAlignment, -1);
	}
	
	public static QAPairOld parseNumberedQAPair(String question, String answer) {
		QAPairOld qa = new QAPairOld(question, answer);
		for (int i = 0; i < qa.questionTokens.length; i++) {
			String token = qa.questionTokens[i];
			int split = token.lastIndexOf('/');
			if (split != -1) {
				qa.questionTokens[i] = token.substring(0, split);
				qa.questionAlignment[i] = Integer.parseInt(
						token.substring(split + 1));
			}
		}
		for (int i = 0; i < qa.answerTokens.length; i++) {
			String token = qa.answerTokens[i];
			int split = token.lastIndexOf('/');
			if (split != -1) {
				qa.answerTokens[i] = token.substring(0, split);
				qa.answerAlignment[i] = Integer.parseInt(
						token.substring(split + 1));
			}
		}
		return qa;
	}
	
	public void setAlignedProposition(Proposition prop) {
		int start = prop.span[0], end = prop.span[1];
		int len = end - start;
		propositionTokens = new String[len];
		propositionAlignment = new int[len];
		for (int i = 0; i < len; i++) {
			propositionTokens[i] = prop.sentence.getTokenString(i + start);
			propositionAlignment[i] = i + start; 
		}
	}
	
	public int getPropositionHead() {
		if (this.propositionTokens == null) {
			return -1;
		}
		return propositionAlignment[propositionAlignment.length - 1];
	}
	
	public String getQuestionString() {
		return StrUtils.join(" ", questionTokens);
	}
	
	public String getAnswerString() {
		return StrUtils.join(" ", answerTokens);
	}
	
	@Override
	public String toString() {
		return StrUtils.join(" ", questionTokens) + "\t" +
			   StrUtils.join(" ", answerTokens);
	}
	
	public void printAlignment() {
		for (int i = 0; i < questionTokens.length; i++) {
			System.out.print(questionTokens[i] + "\t");
		}
		System.out.println();
		for (int i = 0; i < questionTokens.length; i++) {
			System.out.print(questionAlignment[i] + "\t");
		}
		System.out.println();
		for (int i = 0; i < answerTokens.length; i++) {
			System.out.print(answerTokens[i] + "\t");
		}
		System.out.println();
		for (int i = 0; i < answerTokens.length; i++) {
			System.out.print(answerAlignment[i] + "\t");
		}
		System.out.println();
	}
	
}
