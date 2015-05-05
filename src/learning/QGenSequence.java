package learning;

import data.Sentence;

public class QGenSequence {
	public int sequenceId;
	public QASample sample;
	public Sentence sentence;
	public int propHead;
	public int[] latticeIds, cliqueIds;
	public boolean isLabeled;
	
	public QGenSequence(int sequenceId, Sentence sentence, QASample sample,
			int[] latticeIds, int[] cliqueIds, boolean isLabeled) {
		this.sequenceId = sequenceId;
		this.sample = sample;
		this.sentence = sentence;
		this.propHead = sample.propHead;
		this.latticeIds = latticeIds;
		this.cliqueIds = cliqueIds;
		this.isLabeled  =isLabeled;
	}
}
