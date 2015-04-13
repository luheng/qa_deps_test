package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AnnotatedSentence {
	public SRLSentence sentence;
	public HashMap<Integer, ArrayList<QAPair>> qaLists;
	public HashSet<String> annotators;
	
	public AnnotatedSentence(SRLSentence sentence) {
		this.sentence = sentence;
		qaLists = new HashMap<Integer, ArrayList<QAPair>>();
		annotators = new HashSet<String>();
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
		/*
		for (QAPair qa0 : qaList) {
			if (qa0.equals(qa)) {
				qa0.cfAnnotationSources.addAll(qa.cfAnnotationSources);
				return false;
			}
		}*/
		qaList.add(qa);
		annotators.add(qa.annotator);
		return true;
	}
}
