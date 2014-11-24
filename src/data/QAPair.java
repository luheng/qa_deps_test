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
		for (int i = 0; i < questionTokens.length; i++) {
			questionTokens[i] = questionTokens[i].trim();
		}
		for (int i = 0; i < answerTokens.length; i++) {
			answerTokens[i] = answerTokens[i].trim();
		}
		questionAlignment = new int[questionTokens.length];
		answerAlignment = new int[answerTokens.length];
		Arrays.fill(questionAlignment, -1);
		Arrays.fill(answerAlignment, -1);
	}
	
	public static QAPair parseNumberedQAPair(String question, String answer) {
		QAPair qa = new QAPair(question, answer);
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
