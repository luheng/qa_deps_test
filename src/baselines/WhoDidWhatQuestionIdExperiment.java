package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import config.DataConfig;
import config.QuestionIdConfig;
import util.StringUtils;
import annotation.QuestionEncoder;
import learning.KBestParseRetriever;
import learning.QASample;
import learning.QuestionIdDataset;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import evaluation.F1Metric;

public class WhoDidWhatQuestionIdExperiment {
	private QuestionIdConfig config;
	private Corpus baseCorpus; 
	
	CountDictionary slotDict, tempDict;
	
	private QuestionIdDataset trainSet;
	private ArrayList<QuestionIdDataset> testSets;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	public WhoDidWhatQuestionIdExperiment(String questionIdConfigPath)
			throws IOException {
		config = questionIdConfigPath.isEmpty() ? new QuestionIdConfig():
					new QuestionIdConfig(questionIdConfigPath);
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new ArrayList<QuestionIdDataset>();
		
		// ********** Config and load QA Data ********************
		trainSet = new QuestionIdDataset(baseCorpus,
				StringUtils.join("_", config.trainSets));
		for (String dsName : config.trainSets) {
			trainSet.loadData(DataConfig.getDataset(dsName));
		}
		for (String dsName : config.testSets) {
			QuestionIdDataset ds = new QuestionIdDataset(baseCorpus, dsName);
			ds.loadData(DataConfig.getDataset(dsName));
			testSets.add(ds);
		}
		
		// ************ Extract slot labels and templates **************
		slotDict = new CountDictionary();
		tempDict = new CountDictionary();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					String[] temp = QuestionEncoder.getLabels(qa.questionWords);
					slotDict.addString(temp[0]);
					tempDict.addString(StringUtils.join("\t", "_", temp));
				}
			}
		}
		slotDict = new CountDictionary(slotDict, config.minQuestionLabelFreq);
	
		// *********** Generate training/test samples **********
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper =
					new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, slotDict);
			for (QuestionIdDataset ds : testSets) {
				ds.generateSamples(syntaxHelper, slotDict);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.samples);
				ostream.flush();
				ostream.close();
				for (QuestionIdDataset ds : testSets) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(getSampleFileName(ds)));
					ostream.writeObject(ds.samples);
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
     			trainSet.loadSamples(getSampleFileName(trainSet));
     			for (QuestionIdDataset ds : testSets) {
					ds.loadSamples(getSampleFileName(ds));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}		
	}
	
	public double[][] predict(boolean generateQuestions) {
		if (generateQuestions) {
			// TODO: baseline qgen?
		}
		
		double[][] results = new double[testSets.size() + 1][];
		results[0] = predictAndEvaluate(trainSet);
		for (int d = 0; d < testSets.size(); d++) {
			QuestionIdDataset ds = testSets.get(d);
			results[d+1] = predictAndEvaluate(ds);
		}
		return results;
	}
	
	private double[] predictAndEvaluate(QuestionIdDataset ds) {
		F1Metric f1 = new F1Metric();
		int numCorrect = 0;
		int l1 = slotDict.lookupString("A0=someone"),
			l2 = slotDict.lookupString("A1=something");
		System.out.println(l1 + ", " + l2);
		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int labelId = sample.questionLabelId;
			int gold = sample.isPositiveSample ? 1 : -1;
			int pred = (labelId == l1 || labelId == l2) ? 1 : -1;
			f1.numGold += (gold > 0 ? 1 : 0);
			f1.numProposed += (pred > 0 ? 1 : 0);
			f1.numMatched += ((gold > 0 && pred > 0) ? 1 : 0);
			numCorrect += (gold == pred ? 1 : 0);
		}
		return new double[] {1.0 * numCorrect / ds.samples.size(),
				f1.precision(), f1.recall(), f1.f1()}; 	
	}
	
	public static void main(String[] args) {
		WhoDidWhatQuestionIdExperiment exp = null;
		String questionIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new WhoDidWhatQuestionIdExperiment(questionIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		double[][] results = exp.predict(false /* generate question */);
		System.out.println(String.format(
				"Training accuracy on %s:\t%s",
					exp.trainSet.datasetName,
					StringUtils.doubleArrayToString("\t", results[0])));
		for (int j = 0; j < exp.testSets.size(); j++) {
			QuestionIdDataset ds = exp.testSets.get(j);
			System.out.println(String.format(
					"Testing accuracy on %s:\t%s",
						ds.datasetName,
						StringUtils.doubleArrayToString("\t", results[j+1])));
		}
	}
}