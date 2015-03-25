package data;

import java.util.ArrayList;
import java.util.HashMap;

public class AnnotatedSentence {
	public SRLSentence sentence;
	public HashMap<Integer, ArrayList<QAPair>> qaLists;
	
	public AnnotatedSentence(SRLSentence sentence) {
		this.sentence = sentence;
		qaLists = new HashMap<Integer, ArrayList<QAPair>>();
	}
	
	public boolean addProposition(int propHead) {
		if (qaLists.containsKey(propHead)) {
			return false;
		}
		qaLists.put(propHead, new ArrayList<QAPair>());
		return true;
	}
	
	public boolean addQAPair(int propHead, QAPair qa) {
		if (!qaLists.containsKey(propHead)) {
			return false;
		}
		ArrayList<QAPair> qaList = qaLists.get(propHead);
		for (QAPair qa0 : qaList) {
			if (qa0.equals(qa)) {
				qa0.cfAnnotationSources.addAll(qa.cfAnnotationSources);
				return false;
			}
		}
		qaList.add(qa);
		return true;
	}
}
