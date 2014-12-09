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
		this.predicates = new ArrayList<Predicate>();
		for (Predicate pred : predicates) {
			Predicate newPred = pred.clone();
			newPred.sentence = this;
			this.predicates.add(newPred);
		}
	}
	
	public void addPedicate(Predicate pred) {
		pred.sentence = this;
		this.predicates.add(pred);
	}
	
	@Override
	public String toString() {
		String str = super.toString() + "\n";
		for (Predicate pred : predicates) {
			str += pred.toString();
		}
		return str;
	}
}
