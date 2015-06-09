package qg;

import java.util.ArrayList;

import learning.QASample;
import data.Sentence;

public class MultiSequence {
	public int sequenceId;
	public Sentence sentence;
	public int propHead;
	public ArrayList<QASample> samples;
	public ArrayList<int[]> latticeIds, cliqueIds;
	public boolean isLabeled;
	
	public MultiSequence(int sequenceId, Sentence sentence, int propHead,
			boolean isLabeled) {
		this.sequenceId = sequenceId;
		this.sentence = sentence;
		this.propHead = propHead;
		this.isLabeled  =isLabeled;
		samples = new ArrayList<QASample>();
		latticeIds = new ArrayList<int[]>();
		cliqueIds = new ArrayList<int[]>();
	}
	
	public void addSequence(QASample sample, int[] lattice, int[] clique) {
		samples.add(sample);
		latticeIds.add(lattice);
		cliqueIds.add(clique);
	}

	public int numSequences() {
		return samples.size();
	}
}
