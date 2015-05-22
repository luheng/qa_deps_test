package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import config.DataConfig;
import config.QuestionIdConfig;
import util.StrUtils;
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
	private QuestionIdDataset trainSet;
	private ArrayList<QuestionIdDataset> testSets;
	private CountDictionary labelDict;
	
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
				StrUtils.join("_", config.trainSets));
		for (String dsName : config.trainSets) {
			trainSet.loadData(DataConfig.getDataset(dsName));
		}
		for (String dsName : config.testSets) {
			QuestionIdDataset ds = new QuestionIdDataset(baseCorpus, dsName);
			ds.loadData(DataConfig.getDataset(dsName));
			testSets.add(ds);
		}
		
		labelDict = new CountDictionary();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				HashMap<String, String> slots = new HashMap<String, String>();
				for (QAPair qa : sent.qaLists.get(propHead)) {
					String[] temp = QuestionEncoder.getLabels(qa.questionWords);
					for (String lb : temp) {
						if (!lb.contains("=")) {
							continue;
						}
						String pfx = lb.split("=")[0];
						String val = lb.split("=")[1];
						if (slots.containsKey(pfx) &&
							!slots.get(pfx).equals(val)) {
							slots.put(pfx, "something");
						} else {
							slots.put(pfx, val);
						}
						if (!config.aggregateLabels) {
							labelDict.addString(lb);
						}
					}
				}
				if (config.aggregateLabels) {
					for (String pfx : slots.keySet()) {
						labelDict.addString(pfx + "=" + slots.get(pfx));
					}
				}
			}
		}
		assert (config.minQuestionLabelFreq == 1);
		labelDict = new CountDictionary(labelDict, config.minQuestionLabelFreq);
		labelDict.prettyPrint();
		// *********** Generate training/test samples **********
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper =
					new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, labelDict, config.aggregateLabels);
			for (QuestionIdDataset ds : testSets) {
				ds.generateSamples(syntaxHelper, labelDict, config.aggregateLabels);
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
		int l1 = labelDict.lookupString("W0=someone"),
			l2 = labelDict.lookupString("W1=something");
		HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>
			predLabels = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>(),
			goldLabels = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		
		for (AnnotatedSentence annotSent : ds.sentences) {
			int sid = annotSent.sentence.sentenceID;
			predLabels.put(sid, new HashMap<Integer, ArrayList<Integer>>());
			goldLabels.put(sid, new HashMap<Integer, ArrayList<Integer>>());
			for (int pid : annotSent.qaLists.keySet()) {
				predLabels.get(sid).put(pid, new ArrayList<Integer>());
				goldLabels.get(sid).put(pid, new ArrayList<Integer>());
			}
		}
		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int labelId = sample.questionLabelId;
			int gold = sample.isPositiveSample ? 1 : -1;
			int pred = (labelId == l1 || labelId == l2) ? 1 : -1;
			f1.numGold += (gold > 0 ? 1 : 0);
			f1.numProposed += (pred > 0 ? 1 : 0);
			f1.numMatched += ((gold > 0 && pred > 0) ? 1 : 0);
			numCorrect += (gold == pred ? 1 : 0);			
			int sid = sample.sentenceId;
			int pid = sample.propHead;
			if (gold > 0) {
				goldLabels.get(sid).get(pid).add(labelId);
			}
			if (pred > 0) {
				predLabels.get(sid).get(pid).add(labelId);
			}
		}
		/*
		for (AnnotatedSentence annotSent : ds.sentences) {
			int sid = annotSent.sentence.sentenceID;
			for (int pid : annotSent.qaLists.keySet()) {
				ArrayList<Integer> gold = goldLabels.get(sid).get(pid),
								   pred = predLabels.get(sid).get(pid);
				System.out.print("\nGold:\t");
				for (int id : gold) {
					System.out.print(labelDict.getString(id) + "\t");
				}
				System.out.print("\nPred:\t");
				for (int id : pred) {
					System.out.print(labelDict.getString(id) + "\t");
				}

			}
		}
		*/
		// Return: micro-macro
		return new double[] {
				1.0 * numCorrect / ds.samples.size(),
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
					StrUtils.doubleArrayToString("\t", results[0])));
		for (int j = 0; j < exp.testSets.size(); j++) {
			QuestionIdDataset ds = exp.testSets.get(j);
			System.out.println(String.format(
					"Testing accuracy on %s:\t%s",
						ds.datasetName,
						StrUtils.doubleArrayToString("\t", results[j+1])));
		}
	}
}