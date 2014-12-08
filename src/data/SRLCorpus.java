package data;

public class SRLCorpus extends DepCorpus {
	
	public SRLCorpus(String corpusName) {
		super(corpusName);
	}
	
	public SRLCorpus(String corpusName, SRLCorpus baseCorpus) {
		super(corpusName);
		this.corpusName = corpusName;
		this.wordDict = baseCorpus.wordDict;
		this.posDict = baseCorpus.posDict;
		this.depDict = baseCorpus.depDict;
	}
	
	public void loadCoNLL2009Data(String corpusFilename) {
		
	}

}
