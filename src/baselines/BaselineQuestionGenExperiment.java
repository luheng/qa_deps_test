package baselines;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import config.DataConfig;
import config.QuestionGenConfig;
import util.StringUtils;
import annotation.QuestionEncoder;
import learning.KBestParseRetriever;
import learning.QASample;
import learning.QuestionIdDataset;
import learning.QuestionIdFeatureExtractor;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import experiments.LiblinearHyperParameters;

public class BaselineQuestionGenExperiment {

	private QuestionGenConfig config;
	private Corpus baseCorpus; 
	private QuestionIdFeatureExtractor featureExtractor;
	
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	public BaselineQuestionGenExperiment() throws IOException {
		config = new QuestionGenConfig();
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new HashMap<String, QuestionIdDataset>();
		
		// ********** Load QA Data ********************
		if (config.trainWithWiki) {
			trainSet = new QuestionIdDataset(baseCorpus, "wiki1-train");
			testSets.put("prop-train", new QuestionIdDataset(baseCorpus, "prop-train"));
			trainSet.loadData(DataConfig.get("wikiQATrainFilename"));
			testSets.get("prop-train").loadData(DataConfig.get("propbankQATrainFilename"));
		} else {
			trainSet = new QuestionIdDataset(baseCorpus, "prop-train");
			testSets.put("wiki1-train", new QuestionIdDataset(baseCorpus, "wiki1-train"));
			trainSet.loadData(DataConfig.get("propbankQATrainFilename"));
			testSets.get("wiki1-train").loadData(DataConfig.get("wikiQATrainFilename"));
		}

	}
	
	public static void main(String[] args) {
		BaselineQuestionGenExperiment exp = null;
		try {
			exp = new BaselineQuestionGenExperiment();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	
	}
}
