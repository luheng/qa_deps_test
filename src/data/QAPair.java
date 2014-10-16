package data;

import java.util.Arrays;

import util.StringUtils;

public class QAPair {
	public String[] questionTokens, answerTokens;
	// Contains indices of words in the original sentence. -1 means unaligned.
	public int[] questionAlignment, answerAlignment;
	
	public QAPair(String question, String answer) {
		questionTokens = question.split("\\s+");
		answerTokens = answer.split("\\s+");
		questionAlignment = new int[questionTokens.length];
		answerAlignment = new int[answerTokens.length];
		Arrays.fill(questionAlignment, -1);
		Arrays.fill(answerAlignment, -1);
	}
	
	@Override
	public String toString() {
		return StringUtils.join(" ", questionTokens) + "\t" +
			   StringUtils.join(" ", answerTokens);
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
