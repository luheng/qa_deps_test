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
	
	// TODO: QA alignment
}
