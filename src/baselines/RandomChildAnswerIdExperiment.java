package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import config.AnswerIdConfig;
import config.DataConfig;
import learning.AnswerIdDataset;
import learning.KBestParseRetriever;
import learning.QASample;
import data.Corpus;
import edu.stanford.nlp.trees.TypedDependency;
import util.StringUtils;

public class RandomChildAnswerIdExperiment {
	private AnswerIdConfig config;
	private Corpus baseCorpus; 
	private AnswerIdDataset trainSet;
	private ArrayList<AnswerIdDataset> testSets;
	private Random random;
	private final int randomSeed = 12345;
	
	private String getSampleFileName(AnswerIdDataset ds) {
		if (config.useSpanBasedSamples) {
			return ds.datasetName + ".sp.k" + config.kBest + ".smp";
		} else {
			return ds.datasetName + ".k" + config.kBest + ".smp";
		}
	}
	
	public RandomChildAnswerIdExperiment(String answerIdConfigPath)
			throws IOException, ClassNotFoundException {
		config = answerIdConfigPath.isEmpty() ? new AnswerIdConfig() :
						new AnswerIdConfig(answerIdConfigPath);
		random = new Random(randomSeed);
		
		baseCorpus = new Corpus("qa-exp-corpus");
		trainSet = new AnswerIdDataset(baseCorpus,
				StringUtils.join("_", config.trainSets));
		testSets = new ArrayList<AnswerIdDataset>();
		
		// ********** Config and load QA Data ********************
		for (String dsName : config.trainSets) {
			trainSet.loadData(DataConfig.getDataset(dsName));
		}
		for (String dsName : config.testSets) {
			AnswerIdDataset ds = new AnswerIdDataset(baseCorpus, dsName);
			ds.loadData(DataConfig.getDataset(dsName));
			testSets.add(ds);
		}
		
		// *********** Generate training/test samples **********
		String dataPath = DataConfig.get("tempOutputDatapath");
		ObjectOutputStream ostream = null;
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, config.useSpanBasedSamples);
			ostream = new ObjectOutputStream(
					new FileOutputStream(dataPath + getSampleFileName(trainSet)));
			ostream.writeObject(trainSet.samples);
			ostream.flush();
			ostream.close();
			for (AnswerIdDataset ds : testSets) {
				ds.generateSamples(syntaxHelper, config.useSpanBasedSamples);
				ostream = new ObjectOutputStream(
						new FileOutputStream(dataPath + getSampleFileName(ds)));
				ostream.writeObject(ds.samples);
				ostream.flush();
				ostream.close();
			}
		} else {
			trainSet.loadSamples(dataPath + getSampleFileName(trainSet));
 			for (AnswerIdDataset ds : testSets) {
				ds.loadSamples(dataPath + getSampleFileName(ds));
			}
		}
		trainSet.assignLabels();
		for (AnswerIdDataset ds : testSets) {
			ds.assignLabels();
		}
	}
	
	public double[][] predict() {
		double[][] results = new double[testSets.size() + 1][];
		results[0] = predictAndEvaluate(trainSet);
		for (int i = 0; i < testSets.size(); i++) {
			AnswerIdDataset ds = testSets.get(i);
			results[i + 1] = predictAndEvaluate(ds);
		}
		return results;
	}
	
	private double[] predictAndEvaluate(AnswerIdDataset ds) {
		int numQuestions = ds.answerFlags.length;
		double[][] predScores = new double[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			predScores[i] = new double[ds.answerFlags[i].length];
			Arrays.fill(predScores[i], -1);
		}
		HashSet<Integer> evalQIds = new HashSet<Integer>();

		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int qid = sample.questionId,
				aid = sample.answerWordPosition;
			for (TypedDependency dep : sample.kBestParses.get(0)) {
				if (dep.gov().index() == sample.propHead + 1&&
					dep.dep().index() == aid + 1) {
					predScores[qid][aid] = random.nextDouble();
				}
			}
			evalQIds.add(qid);
		}
		
		int numMatched = 0;
		int numContained = 0;
		double avgMatchedWords = .0;		
		for (int qid = 0; qid < numQuestions; qid++) {
			if (!evalQIds.contains(qid)) {
				continue;
			}
			int length = predScores[qid].length;
			int bestIdx = -1;
			for (int i = 0; i < length; i++) {
				if (predScores[qid][i] < 1e-6) {
					continue;
				}
				if (bestIdx < 0 ||
					predScores[qid][i] > predScores[qid][bestIdx]) {
					bestIdx = i;
				}
			}
			if (bestIdx > -1 && ds.answerFlags[qid][bestIdx] > 0) {
				numContained ++;
			}
			if (bestIdx > -1 && ds.answerHeads[qid][bestIdx] > 0) {
				numMatched ++;
			}
		}
		
		double[] result = new double[2];
		if (config.useSpanBasedSamples) {
			result[0] = avgMatchedWords / evalQIds.size();
		} else {
			result[0] = 1.0 * numMatched /evalQIds.size();
			result[1] = 1.0 * numContained /evalQIds.size();
		}
		return result;				
	}
	
	public static void main(String[] args) {
		RandomChildAnswerIdExperiment exp = null;
		String answerIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new RandomChildAnswerIdExperiment(answerIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		double[][] results = exp.predict();
		System.out.println(String.format(
				"Training accuracy on %s:\t%.4f\t%.4f",
					exp.trainSet.datasetName, results[0][0], results[0][1]));
		for (int j = 0; j < exp.testSets.size(); j++) {
			AnswerIdDataset ds = exp.testSets.get(j);
			System.out.println(String.format(
					"Testing accuracy on %s:\t%.4f\t%.4f",
						ds.datasetName, results[j+1][0], results[j+1][1]));
		}
	}
}
