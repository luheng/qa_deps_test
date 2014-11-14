package data;

import java.util.ArrayList;

public class AnnotatedSentence {
	public DepSentence depSentence;
	public ArrayList<QAPair> qaList;
	
	public AnnotatedSentence(DepSentence sentence) {
		this.depSentence = sentence;
		qaList = new ArrayList<QAPair>();
	}
	
	public void addQA(QAPair qa) {
		this.qaList.add(qa);
	}
	
	@Override
	public String toString() {
		String retStr = depSentence.sentenceID + "\t" +
					    depSentence.getTokensString() + "\n";
		for (int i = 0; i < qaList.size(); i++) {			
			retStr += "\tQA" + i + "\t" + qaList.get(i).toString() + "\n";
		}
		return retStr;
	}
 
	/**
	 * Adapted from Luke's code ...
	 * @param gold
	 * @param scorer
	 * @return
	 */
	public void prettyPrintDebugString(int[] prediction, double[][] scores){
		for (int i = 0; i < depSentence.length; i++) {
			int predParent = prediction[i],
				goldParent = depSentence.parents[i];
			System.out.print(
					String.format("%2d %-15s %2d (%2.2f) %2d (%2.2f)",
							i,
							depSentence.getTokenString(i), 
							predParent,
							scores[predParent + 1][i + 1],
							goldParent,
							scores[goldParent + 1][i + 1]));
			if (goldParent != predParent){
				System.out.print(" ** ");
			}			
			System.out.println();
		}
	}
}
