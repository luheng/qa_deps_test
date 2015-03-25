package data;

import java.util.ArrayList;

public class AnnotatedDepSentence {
	public DepSentence depSentence;
	public ArrayList<QAPairOld> qaList;
	
	public AnnotatedDepSentence(DepSentence sentence) {
		this.depSentence = sentence;
		qaList = new ArrayList<QAPairOld>();
	}
	
	public void addQA(QAPairOld qa) {
		this.qaList.add(qa);
	}
	
	@Override
	public String toString() {
		String retStr = depSentence.sentenceID + "\t" +
					    depSentence.getTokensString() + "\n";
		for (int i = 0; i < qaList.size(); i++) {			
			retStr += "QA" + i + "\t" + qaList.get(i).toString() + "\n";
		}
		return retStr;
	}
	
	public void prettyPrintAlignment() {
		System.out.print(depSentence.sentenceID + "\t");
		for (int i = 0; i < depSentence.length; i++) {
			System.out.print(String.format("%s(%d) ",
					depSentence.getTokenString(i), i));
		}
		System.out.println();
		
		for (int i = 0; i < qaList.size(); i++) {
			QAPairOld qa = qaList.get(i);
			System.out.print(String.format("QA%d\t", i));
			if (qa.propositionTokens != null) {
				for (int j = 0; j < qa.propositionTokens.length; j++) {
					System.out.print(String.format("%s(%d) ",
							qa.propositionTokens[j],
							qa.propositionAlignment[j]));
				}
				System.out.print("\t");
				for (int j = 0; j < qa.questionTokens.length; j++) {
					if (qa.questionAlignment[j] == -1) {
						System.out.print(qa.questionTokens[j] + " ");
					} else {
						System.out.print(String.format("%s(%d) ",
								qa.questionTokens[j], qa.questionAlignment[j]));
					}
				}
				System.out.print("\t");
				for (int j = 0; j < qa.answerTokens.length; j++) {
					if (qa.answerAlignment[j] == -1) {
						System.out.print(qa.answerTokens[j] + " ");
					} else {
						System.out.print(String.format("%s(%d) ",
								qa.answerTokens[j], qa.answerAlignment[j]));
					}
				}
				System.out.println();
			}
		}
	}
}
