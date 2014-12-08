package data;

public class SRLSentence extends DepSentence {

	int[] lemmas;
	
	public SRLSentence(int[] tokens, int[] postags, int[] parents,
			int[] deptags, DepCorpus corpus, int sentenceID) {
		super(tokens, postags, parents, deptags, corpus, sentenceID);
		
	}
	
	

}
