package data;

public class DepSentence {
	public int[] tokens, postags, parents, deptags;
	public int length;
	public DepCorpus corpus;
	
	public DepSentence(int[] tokens, int[] postags, int[] parents,
					   int[] deptags, DepCorpus corpus) {
		this.tokens = tokens;
		this.postags = postags;
		this.parents = parents;
		this.deptags = deptags;
		this.length = tokens.length;
		this.corpus = corpus;
	}
	
	// TODO (luheng): Pretty print dependency sentence.
	@Override
	public String toString() {
		return "";
	}
}
