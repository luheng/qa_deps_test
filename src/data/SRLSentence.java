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
	
	public String[][] getSemanticArcs() {
		SRLCorpus srlCorpus = (SRLCorpus) this.corpus;
		CountDictionary propDict = srlCorpus.propDict,
					    argModDict = srlCorpus.argModDict;
		String[][] arcs = new String[length + 1][length + 1];
		for (int i = 0; i < arcs.length; i++) {
			for (int j = 0; j < arcs[i].length; j++) {
				arcs[i][j] = "";
			}
		}
		for (Proposition prop : propositions) {
			int pid = prop.propID + 1;
			arcs[0][pid] = propDict.index2str.get(prop.propType);
			for (int i = 0; i < prop.argIDs.length; i++) {
				arcs[pid][prop.argIDs[i] + 1] = String.format("A%d", i);
			}
			for (int i = 0; i < prop.argModIDs.size(); i++) {
				arcs[pid][prop.argModIDs.get(i) + 1] =
						argModDict.index2str.get(prop.argModTypes.get(i));
			}
		}
		return arcs;
	}
}
