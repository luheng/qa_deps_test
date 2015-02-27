package data;

import java.util.ArrayList;
import java.util.HashMap;

public class AnnotatedSentence {
	public SRLSentence sentence;
	public HashMap<Integer, ArrayList<StructuredQAPair>> qaLists;
	
	public AnnotatedSentence(SRLSentence sentence) {
		this.sentence = sentence;
		qaLists = new HashMap<Integer, ArrayList<StructuredQAPair>>();
	}
	
	public boolean addProposition(int propHead) {
		if (qaLists.containsKey(propHead)) {
			return false;
		}
		qaLists.put(propHead, new ArrayList<StructuredQAPair>());
		return true;
	}
	
	public boolean addQAPair(int propHead, StructuredQAPair qa) {
		if (!qaLists.containsKey(propHead)) {
			return false;
		}
		ArrayList<StructuredQAPair> qaList = qaLists.get(propHead);
		for (StructuredQAPair qa0 : qaList) {
			if (qa0.equals(qa)) {
				qa0.cfAnnotationSources.addAll(qa.cfAnnotationSources);
				return false;
			}
		}
		qaList.add(qa);
		return true;
	}
}
