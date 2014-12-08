package data;

import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DepCorpus {
	public String corpusName;
	public CountDictionary wordDict, posDict, depDict;
	public ArrayList<DepSentence> sentences;
	
	public DepCorpus(String corpusName) {
		this.corpusName = corpusName;
		wordDict = new CountDictionary();
		posDict = new CountDictionary();
		depDict = new CountDictionary();
		sentences = new ArrayList<DepSentence>();
	}
	
	// Using the dictionary of the other corpus. i.e. 
	// testCorpus = new DepCorpus(trainCorpus).
	public DepCorpus(String corpusName, DepCorpus baseCorpus) {
		this.corpusName = corpusName;
		this.wordDict = baseCorpus.wordDict;
		this.posDict = baseCorpus.posDict;
		this.depDict = baseCorpus.depDict;
	}
	
	public void loadUniversalDependencyData(String corpusFilename)
			throws NumberFormatException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(corpusFilename)));
		
		String currLine;
		TIntArrayList tokens = new TIntArrayList(),
				      postags = new TIntArrayList(),
				      parents = new TIntArrayList(),
				      deptags = new TIntArrayList();
		
		while ((currLine = reader.readLine()) != null) {
			String[] columns = currLine.split("\\s+");
			if (columns.length < 8) {
				// Add new sentence
				int nextSentenceID = sentences.size();
				sentences.add(new DepSentence(tokens.toArray(),
						                      postags.toArray(),
						                      parents.toArray(),
						                      deptags.toArray(),
						                      this, nextSentenceID));
				tokens.clear();
				postags.clear();
				parents.clear();
				deptags.clear();
			} else {
				// Add new token according to CoNLL format:
				// 1   In  _   ADP IN  _   45  adpmod  _   _
				tokens.add(wordDict.addString(columns[1]));
				postags.add(posDict.addString(columns[3]));
				parents.add(Integer.valueOf(columns[6]) - 1);
				deptags.add(depDict.addString(columns[7]));
			}
		}
		if (tokens.size() > 0) {
			int nextSentenceID = sentences.size();
			sentences.add(new DepSentence(tokens.toArray(),
                    postags.toArray(),
                    parents.toArray(),
                    deptags.toArray(),
                    this, nextSentenceID));
		} 
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.\n",
				                         sentences.size(), corpusFilename));
	}
}
