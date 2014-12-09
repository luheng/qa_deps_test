package data;

import experiments.ExperimentUtils;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SRLCorpus extends DepCorpus {

	CountDictionary lemmaDict, argModDict, propDict;
	
	public SRLCorpus(String corpusName) {
		super(corpusName);
		this.lemmaDict = new CountDictionary();
		this.argModDict = new CountDictionary();
		this.propDict = new CountDictionary();
	}
	
	public SRLCorpus(String corpusName, SRLCorpus baseCorpus) {
		super(corpusName, baseCorpus);
		this.lemmaDict = baseCorpus.lemmaDict;
		this.argModDict = baseCorpus.argModDict;
		this.propDict = baseCorpus.propDict;
	}
	
	/** The CoNLL 2009 File Format:
	 *  0: ID
		1: Form
		2: Lemma
		3: Plamma
		4: Pos
		5: Ppos
		6: Feat
		7: Pfeat
		8: Head
		9: Phead
		10: Deprel
		11: Pdeprel
		12: Fillpred
		13: pred
		14: Apreds
	 */
	public void loadCoNLL2009Data(String corpusFilename, boolean readGold)
			throws NumberFormatException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(corpusFilename)));
		
		String currLine;
		TIntArrayList tokens = new TIntArrayList(),
				      postags = new TIntArrayList(),
				      parents = new TIntArrayList(),
				      deptags = new TIntArrayList(),
				      lemmas = new TIntArrayList();
		ArrayList<Proposition> propositions = new ArrayList<Proposition>();
		int numPropositions = -1, propCount = -1;
		
		while ((currLine = reader.readLine()) != null) {
			String[] columns = currLine.split("\\s+");
			if (columns.length < 13) {
				// Add new sentence
				int nextSentenceID = sentences.size();
				sentences.add(new SRLSentence(tokens.toArray(),
											  lemmas.toArray(),
						                      postags.toArray(),
						                      parents.toArray(),
						                      deptags.toArray(),
						                      propositions,
						                      this, nextSentenceID));
				tokens.clear();
				lemmas.clear();
				postags.clear();
				parents.clear();
				deptags.clear();
				propositions.clear();
				numPropositions = -1;
				propCount = -1;
			} else {
				tokens.add(wordDict.addString(columns[1]));
				if (readGold) {
					lemmas.add(lemmaDict.addString(columns[2]));
					postags.add(posDict.addString(columns[4]));
					parents.add(Integer.valueOf(columns[8]) - 1);
					deptags.add(depDict.addString(columns[10]));
				} else {
					lemmas.add(lemmaDict.addString(columns[3]));
					postags.add(posDict.addString(columns[5]));
					parents.add(Integer.valueOf(columns[9]) - 1);
					deptags.add(depDict.addString(columns[11]));
				}
				if (numPropositions == -1) {
					numPropositions = columns.length - 14;
					propCount = 0;
					for (int i = 0; i < numPropositions; i++) {
						propositions.add(new Proposition());
					}
				}
				// Add predicate.
				int wordCount = tokens.size() - 1;
				if (columns[12].equals("Y")) {
					propositions.get(propCount).setPredicate(wordCount,
							propDict.addString(columns[13]));
					propCount ++;
				}
				for (int i = 0; i < numPropositions; i++) {
					String info = columns[14 + i];
					if (info.startsWith("AM")) {
						propositions.get(i).addArgumentModifier(wordCount,
								argModDict.addString(info));
					} else if (info.startsWith("A")) {
						propositions.get(i).addArgument(wordCount,
								Integer.parseInt(info.substring(1)));
					}
				}
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
	
	
	public static void main(String[] args) {
		SRLCorpus corpus = new SRLCorpus("trial");
		try {
			corpus.loadCoNLL2009Data(ExperimentUtils.conll2009TrialFilename,
									 true);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (DepSentence s : corpus.sentences) {
			SRLSentence sentence = (SRLSentence) s;
			System.out.println(sentence.toString());
		}
	}

}
