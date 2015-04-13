package data;

import java.util.ArrayList;

import util.LatticeUtils;

public class SRLSentence extends DepSentence {
	int[] lemmas;
	public ArrayList<Proposition> propositions;
	
	public SRLSentence(int[] tokens, int[] lemmas, int[] postags, int[] parents,
					   int[] deptags, ArrayList<Proposition> propositions,
					   SRLCorpus corpus, int sentenceID) {
		super(tokens, postags, parents, deptags, corpus, sentenceID);
		this.lemmas = lemmas;
		this.propositions = new ArrayList<Proposition>();
		for (Proposition prop : propositions) {
			Proposition newProp = prop.clone();
			newProp.sentence = this;
			this.propositions.add(newProp);
		}
	}
	
	public void addProposition(Proposition prop) {
		prop.sentence = this;
		this.propositions.add(prop);
	}
	
	@Override
	public String toString() {
		String str = super.toString() + "\n";
		for (Proposition prop : propositions) {
			str += prop.toString();
		}
		return str;
	}
	
	public String[][] getSemanticArcs() {
		SRLCorpus srlCorpus = (SRLCorpus) this.corpus;
		CountDictionary propDict = srlCorpus.propDict,
					    argModDict = srlCorpus.argModDict;
		String[][] arcs = new String[length + 1][length + 1];
		LatticeUtils.fill(arcs, "");
		for (Proposition prop : propositions) {
			int pid = prop.propID + 1;
			arcs[0][pid] = propDict.index2str.get(prop.propType);
			/*
			for (int i = 0; i < prop.argIDs.length; i++) {
				if (prop.argIDs[i] >= 0) {
					arcs[pid][prop.argIDs[i] + 1] = String.format("A%d", i);
				}
			}
			*/
			for (int i = 0; i < prop.argIDs.size(); i++) {
				arcs[pid][prop.argIDs.get(i) + 1] =
						argModDict.index2str.get(prop.argTypes.get(i));
			}
		}
		return arcs;
	}
}
