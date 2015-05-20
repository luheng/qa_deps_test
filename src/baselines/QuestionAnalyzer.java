package baselines;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import config.DataConfig;
import learning.QuestionIdDataset;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;


public class QuestionAnalyzer {

	private boolean trainWithWiki = false;
	
	private Corpus baseCorpus; 
	
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	public QuestionAnalyzer() throws IOException {
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new HashMap<String, QuestionIdDataset>();
		
		// ********** Load QA Data ********************
		if (trainWithWiki) {
			trainSet = new QuestionIdDataset(baseCorpus, "wiki1-train");
			testSets.put("prop-train", new QuestionIdDataset(baseCorpus, "prop-train"));
			
			trainSet.loadData(DataConfig.get("wikiQATrainFilename"));
			testSets.get("prop-train").loadData(DataConfig.get("propbankQATrainFilename"));
		} else {
			trainSet = new QuestionIdDataset(baseCorpus, "prop-train");
			testSets.put("prop-dev", new QuestionIdDataset(baseCorpus, "prop-dev"));
			
			trainSet.loadData(DataConfig.get("propbankQATrainFilename"));
			testSets.get("prop-dev").loadData(DataConfig.get("propbankQADevFilename"));
		}

		CountDictionary slots = new CountDictionary();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				ArrayList<QAPair> qaList = sent.qaLists.get(propHead);
			//	CountDictionary sc = 
				//		QuestionEncoder.encode(sent.sentence, propHead,
					//			sent.qaLists.get(propHead));
				/*
				System.out.println(sent.sentence.getTokensString());
				for (QAPair qa : qaList) {
					System.out.println(qa.getQuestionString() + "\t" + qa.getAnswerString());
				}
				sc.prettyPrint();
				System.out.println("\n");
				*/
			//	for (int i = 0; i < sc.size(); i++) {
			//		slots.addString(sc.getString(i));
			//	}
			}
		}
		slots.prettyPrint();
	}
	
	
	public static void main(String[] args) {
		QuestionAnalyzer exp = null;
		try {
			exp = new QuestionAnalyzer();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		
	}
}
