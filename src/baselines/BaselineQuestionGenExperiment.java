package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import config.DataConfig;
import config.QuestionGenConfig;
import learning.KBestParseRetriever;
import learning.QGenCRF;
import learning.QGenDataset;
import data.Corpus;
import data.VerbInflectionDictionary;

public class BaselineQuestionGenExperiment {

	private QuestionGenConfig config;
	private Corpus baseCorpus; 
	private VerbInflectionDictionary inflDict;
	private QGenCRF crf;
	
	private QGenDataset trainSet;
	private HashMap<String, QGenDataset> testSets;
	
	private final int kBest = 1; // 20
	private boolean regenerateSamples = false;
	
	private String getSampleFileName(QGenDataset ds) {
		return ds.datasetName + ".qgen.k" + kBest + ".smp";
	}
	
	public BaselineQuestionGenExperiment() throws IOException {
		config = new QuestionGenConfig();
		baseCorpus = new Corpus("qa-exp-corpus");
		
		testSets = new HashMap<String, QGenDataset>();
		// ********** Load QA Data ********************
		if (config.trainWithWiki) {
			trainSet = new QGenDataset(baseCorpus, "wiki1-train");
			testSets.put("prop-train", new QGenDataset(baseCorpus, "prop-train"));
			trainSet.loadData(DataConfig.get("wikiQATrainFilename"));
			testSets.get("prop-train").loadData(DataConfig.get("propbankQATrainFilename"));
		} else {
			trainSet = new QGenDataset(baseCorpus, "prop-train");
			testSets.put("wiki1-train", new QGenDataset(baseCorpus, "wiki1-train"));
			trainSet.loadData(DataConfig.get("propbankQATrainFilename"));
			testSets.get("wiki1-train").loadData(DataConfig.get("wikiQATrainFilename"));
		}
		
		// **************** Load samples ****************
		if (regenerateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trainSet.generateSamples(syntaxHelper);
			for (QGenDataset ds : testSets.values()) {
				ds.generateSamples(syntaxHelper);
			}
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.samples);
				ostream.flush();
				ostream.close();
				for (QGenDataset testSet : testSets.values()) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(getSampleFileName(testSet)));
					ostream.writeObject(testSet.samples);
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
	 			trainSet.loadSamples(getSampleFileName(trainSet));
	 			for (QGenDataset testSet : testSets.values()) {
					testSet.loadSamples(getSampleFileName(testSet));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}		
		}
	}
	
	private void run() {
		crf = new QGenCRF(baseCorpus, trainSet, testSets);
		crf.run();
	}
	
	public static void main(String[] args) {
		BaselineQuestionGenExperiment exp = null;
		try {
			exp = new BaselineQuestionGenExperiment();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		exp.run();
	}
}
