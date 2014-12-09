package data;

import java.util.ArrayList;

public class SRLSentence extends DepSentence {
	int[] lemmas;
	ArrayList<Proposition> propositions;
	
	public SRLSentence(int[] tokens, int[] lemmas, int[] postags, int[] parents,
					   int[] deptags, ArrayList<Proposition> predicates,
					   SRLCorpus corpus, int sentenceID) {
		super(tokens, postags, parents, deptags, corpus, sentenceID);
		this.lemmas = lemmas;
		this.propositions = new ArrayList<Proposition>();
		for (Proposition prop : predicates) {
			Proposition newProp = prop.clone();
			newProp.sentence = this;
			this.propositions.add(newProp);
		}
	}
	
	public void addPedicate(Proposition prop) {
		prop.sentence = this;
		this.propositions.add(prop);
	}
	
	@Override
	public String toString() {
		String str = super.toString() + "\n";
		for (Proposition pred : propositions) {
			str += pred.toString();
		}
		return str;
	}
}
