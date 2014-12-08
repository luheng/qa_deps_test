package data;

import java.util.ArrayList;

public class SRLSentence extends DepSentence {
	int[] lemmas;
	ArrayList<Predicate> predicates;
	
	public SRLSentence(int[] tokens, int[] lemmas, int[] postags, int[] parents,
					   int[] deptags, ArrayList<Predicate> predicates,
					   SRLCorpus corpus, int sentenceID) {
		super(tokens, postags, parents, deptags, corpus, sentenceID);
		this.lemmas = lemmas;
		this.predicates = predicates;
		for (Predicate pred : predicates) {
			pred.sentence = this;
		}
	}
	
	public void addPedicate(Predicate pred) {
		pred.sentence = this;
		this.predicates.add(pred);
	}

}
