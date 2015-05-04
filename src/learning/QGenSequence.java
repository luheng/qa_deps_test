package learning;

import data.Sentence;

public class QGenSequence {
	public int sequenceId;
	public Sentence sentence;
	public int propHead;
	public int[] latticeIds, cliqueIds;
	public boolean isLabeled;
	
	public QGenSequence(int sequenceId, Sentence sentence, int propHead,
			int[] latticeIds, int[] cliqueIds, boolean isLabeled) {
		this.sequenceId = sequenceId;
		this.sentence = sentence;
		this.propHead = propHead;
		this.latticeIds = latticeIds;
		this.cliqueIds = cliqueIds;
		this.isLabeled  =isLabeled;
	}
}
