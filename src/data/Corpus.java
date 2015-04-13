package data;

import java.util.ArrayList;

public class Corpus {
	public String corpusName;
	public CountDictionary wordDict;
	public ArrayList<Sentence> sentences;
	
	public Corpus(String corpusName) {
		this.corpusName = corpusName;
		this.wordDict = new CountDictionary();
		this.sentences = new ArrayList<Sentence>();
	}
	
	public Sentence getSentence(int sentId) {
		return sentences.get(sentId);
	}
	
}
