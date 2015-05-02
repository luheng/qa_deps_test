package data;

import experiments.ExperimentUtils;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import config.DataConfig;

public class SRLCorpus extends DepCorpus {

	public CountDictionary lemmaDict, argModDict, propDict;
	
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
	
	public SRLSentence getSentence(int sentId) {
		return (SRLSentence) sentences.get(sentId);
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
	public void loadCoNLL2009Data(String corpusFilename,
			UniversalPostagMap univmap, boolean readGold)
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
				SRLSentence sentence = new SRLSentence(tokens.toArray(),
						  lemmas.toArray(),
	                      postags.toArray(),
	                      parents.toArray(),
	                      deptags.toArray(),
	                      propositions,
	                      this, nextSentenceID);
				sentences.add(sentence);
				/*
				sentences.add(new SRLSentence(tokens.toArray(),
											  lemmas.toArray(),
						                      postags.toArray(),
						                      parents.toArray(),
						                      deptags.toArray(),
						                      propositions,
						                      this, nextSentenceID));*/
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
				String postag = readGold ? columns[4] : columns[5];
				postags.add(posDict.addString(univmap == null ?
						postag : univmap.getUnivPostag(postag)));
				if (readGold) {
					lemmas.add(lemmaDict.addString(columns[2]));
					parents.add(Integer.valueOf(columns[8]) - 1);
					deptags.add(depDict.addString(columns[10]));
				} else {
					lemmas.add(lemmaDict.addString(columns[3]));
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
					propositions.get(propCount).setProposition(wordCount,
							propDict.addString(columns[13]));
					propCount ++;
				}
				for (int i = 0; i < numPropositions; i++) {
					String info = columns[14 + i];
					if (info.startsWith("A") || //info.startsWith("AM") || info.startsWith("AA") ||
						info.startsWith("C-") || info.startsWith("R-")) {
						propositions.get(i).addArgumentModifier(wordCount,
								argModDict.addString(info));
					} /* else if (info.startsWith("A")) {
						try {
							propositions.get(i).addArgument(wordCount,
									Integer.parseInt(info.substring(1)));
						} catch (NumberFormatException e) {
							System.out.println("Error parsing info: " + info);
						}
					} */
					else if (!info.equals("_")) {
						System.out.println("Unrecognized argument:\t" + info);
					}
				}
			}
		}
		if (tokens.size() > 0) {
			int nextSentenceID = sentences.size();
			sentences.add(new SRLSentence(tokens.toArray(),
					lemmas.toArray(),
                    postags.toArray(),
                    parents.toArray(),
                    deptags.toArray(),
                    propositions,
                    this, nextSentenceID));
		} 
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.\n",
				                         sentences.size(), corpusFilename));
	}
	
	
	public static void main(String[] args) {
		SRLCorpus corpus = new SRLCorpus("trial");
		try {
			corpus.loadCoNLL2009Data(DataConfig.get("srlTrainFilename"),
									 null /* univ postag map */,
									 true /* load gold */);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/* Check for duplicate arguments */
		for (int i = 0; i < corpus.sentences.size(); i++) {
			SRLSentence sentence = corpus.getSentence(i);
			for (Proposition prop : sentence.propositions) {
				HashMap<String, Integer> types = new HashMap<String, Integer>();
				for (int j = 0; j < prop.argTypes.size(); j++) {
					String atype = corpus.argModDict.getString(
							prop.argTypes.get(j));
					if (types.containsKey(atype) && !atype.startsWith("AM")) {
						System.out.println(sentence.toString());
						break;
					}
					types.put(atype, 1);
				}
			}
		}
	}

}
