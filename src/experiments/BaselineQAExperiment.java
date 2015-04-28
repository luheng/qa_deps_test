package experiments;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import learning.AnswerIdDataset;
import learning.AnswerIdFeatureExtractor;
import learning.KBestParseRetriever;
import learning.QASample;
import data.Corpus;
import data.QAPair;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import evaluation.AnswerIdEvaluationParameters.EvaluationType;
import evaluation.AnswerIdEvaluationParameters;
import evaluation.AnswerIdEvaluator;
import evaluation.F1Metric;

public class BaselineQAExperiment {

	private String trainFilePath = "data/odesk_s1250.train.qa";
	private String testFilePath = "data/odesk_s1250.test.qa";	
	private String oodTrainFilePath = "data/odesk_wiki1.train.qa";
	private String oodTestFilePath = "data/odesk_wiki1.test.qa";
	
	private String trainSamplesPath = "odesk_s1250_20best.train.qaSamples";
	
	private String featureOutputPath = "feature_weights.tsv";
	
	private final int randomSeed = 12345;
	private int cvFolds = 5, minFeatureFreq = 3, kBest = 1;
	
	private EvaluationType evalType = EvaluationType.topKHead;
	
	private double evalThreshold = 0;
	private int evalKBest = 1;
	
	private Corpus baseCorpus; 
	private AnswerIdFeatureExtractor featureExtractor;
	private AnswerIdEvaluationParameters evalPrm;
	
	private AnswerIdDataset trainSet;
	private HashMap<String, AnswerIdDataset> testSets;
	
	
	public BaselineQAExperiment(boolean generateSamples) {
		baseCorpus = new Corpus("qa-exp-corpus");
		trainSet = new AnswerIdDataset(baseCorpus);
		testSets = new HashMap<String, AnswerIdDataset>();
		testSets.put("prop-test", new AnswerIdDataset(baseCorpus, "prop-test"));
		testSets.put("wiki1-train", new AnswerIdDataset(baseCorpus, "wiki1-train"));
		testSets.put("wiki1-test", new AnswerIdDataset(baseCorpus, "wiki1-test"));
		
		evalPrm = new AnswerIdEvaluationParameters(evalType, evalKBest,
				evalThreshold);
	
		// ********** Load QA Data ********************
		try {
			trainSet.loadData(trainFilePath);
		//	trainSet.loadData(oodTrainFilePath);
			testSets.get("prop-test").loadData(testFilePath);
			testSets.get("wiki1-train").loadData(oodTrainFilePath);
			testSets.get("wiki1-test").loadData(oodTestFilePath);
		} catch (IOException e) {
			e.printStackTrace();	
		}
		
		// *********** Generate training/test samples **********
		if (generateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trainSet.generateSamples(syntaxHelper);
			for (AnswerIdDataset ds : testSets.values()) {
				ds.generateSamples(syntaxHelper);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(trainSamplesPath));
				ostream.writeObject(trainSet.getSamples());
				ostream.flush();
				ostream.close();
				for (String dsName : testSets.keySet()) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(dsName + ".qaSamples"));
					ostream.writeObject(testSets.get(dsName).getSamples());
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				trainSet.loadSamples(trainSamplesPath);
				for (String dsName : testSets.keySet()) {
					testSets.get(dsName).loadSamples(dsName + ".k" + kBest + ".smp");
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		
		sanityCheck1();
		
		// ********** Extract features ***************
/*		ArrayList<QASample> allSamples = new ArrayList<QASample>();
		allSamples.addAll(trainSet.getSamples());
		for (AnswerIdDataset ds : testSets.values()) {
			allSamples.addAll(ds.getSamples());
		}
*/
		featureExtractor = new AnswerIdFeatureExtractor(baseCorpus, kBest,
				minFeatureFreq);
		featureExtractor.extractFeatures(trainSet.getSamples());
		trainSet.extractFeaturesAndLabels(featureExtractor);
		for (AnswerIdDataset ds : testSets.values()) {
			ds.extractFeaturesAndLabels(featureExtractor);
		}
	}
	
	public void sanityCheck1() {
		// 1. Different datasets should have disjoint sentence Ids.
		System.out.println("======= Sanity Check1: Sentence Overlap =======");
		int overlap = 0;
		HashSet<Integer> sids = trainSet.getSentenceIds();
		for (AnswerIdDataset ds : testSets.values()) {
			HashSet<Integer> sids2 = ds.getSentenceIds();
			for (int sid : sids2) {
				if (sids.contains(sid)) {
					System.out.println("Overlap!!\t" + sid);
					overlap++;
				}
				sids.add(sid);
			}
		}
		if (overlap == 0) {
			System.out.println("======= Sanity Check1 Passed: No Overlap =======");
		} else {
			System.out.println(
					String.format("Sanity Check1 Failed with %d overlapping sentences.",
					overlap));
		}
	}
	
	public LiblinearHyperParameters runCrossValidation(
			ArrayList<LiblinearHyperParameters> cvPrms) {
		ArrayList<Double> cvResults = new ArrayList<Double>();
		double bestAcc = .0;
		LiblinearHyperParameters bestPrm = null;
		
		for (LiblinearHyperParameters prm : cvPrms) {
			System.out.println(prm.toString());
			double res = crossValidate(trainSet, cvFolds, prm);
			cvResults.add(res);
		}
		for (int i = 0; i < cvResults.size(); i++) {
			double acc = cvResults.get(i);
			System.out.println(String.format("%s\t%.5f\n",
					cvPrms.get(i).toString(), acc));
			if (acc > bestAcc) {
				bestAcc = acc;
				bestPrm = cvPrms.get(i);
			}
		}
		return bestPrm;
	}
	
	public void trainAndPredict(LiblinearHyperParameters prm) {
		int numFeatures = featureExtractor.numFeatures();
		Model model = train(
				trainSet.getFeatures(),
				trainSet.getLabels(),
				numFeatures, prm);
		double accuracy = predictAndEvaluate(trainSet, model);
		System.out.println(String.format("Training accuracy:\t %.4f", accuracy));
		for (AnswerIdDataset ds : testSets.values()) {
			accuracy = predictAndEvaluate(ds, model);
			F1Metric oldF1 = predictAndEvaluateOld(ds.getFeatures(), 
					ds.getLabels(), model);
			System.out.println(String.format("Testing accuracy on %s: %.4f",
					ds.datasetName, accuracy));		
			System.out.println("Old f1:\t" + oldF1.toString());
		}
		
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(
					new FileWriter(new File(featureOutputPath)));
			for (int fid = 0; fid < model.getNrFeature(); fid++) {
				double fweight = model.getFeatureWeights()[fid + 1];
				writer.write(String.format("%s\t%.6f\n",
						featureExtractor.featureDict.getString(fid), fweight));
			}
			writer.close();
		} catch (IOException e) {
		}
		
	}
	
	// FIXME: Use a FileWriter ....
	public void outputFeatures(Model model) {		
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("feature_weights.tsv"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (int fid = 0; fid < model.getNrFeature(); fid++) {
			double fweight = model.getFeatureWeights()[fid + 1];
			System.out.println(String.format("%s\t%.6f",
					featureExtractor.featureDict.getString(fid), fweight));
		}
	}
	
	private Model train(Feature[][] features, double[] labels,
			int numFeatures, LiblinearHyperParameters par) {
		Problem training = new Problem();
		training.l = features.length;
		training.n = numFeatures;
		training.x = features;
		training.y = labels;
		Parameter parameter = new Parameter(par.solverType, par.C, par.eps);
		return Linear.train(training, parameter);
	}
	
	private double predictAndEvaluate(AnswerIdDataset ds, Model model) {
		return predictAndEvaluate(ds.getSamples(), ds.getFeatures(), ds, model);
	}
	
	private F1Metric predictAndEvaluateOld(Feature[][] features,
			double[] labels, Model model) {
		int numMatched = 0, numPred = 0, numGold = 0;
		for (int i = 0; i < features.length; i++) {
			int pred = (int) Linear.predict(model, features[i]);
			int gold = (int) labels[i];
			if (gold > 0 && pred > 0) {
				numMatched ++;
			}
			if (gold > 0) {
				numGold ++;
			}
			if (pred > 0) {
				numPred ++;
			}
		}
		return new F1Metric(numMatched, numGold, numPred);
	}
	
	private double predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			AnswerIdDataset ds,
			Model model) {
		int[][] answerFlags = ds.getAnswerFlags();
		int[][] answerHeads = ds.getAnswerHeads();
		ArrayList<QAPair> questions = ds.getQuestions();
		
		int numQuestions = answerFlags.length;
		HashSet<Integer> evalQIds = new HashSet<Integer>();
		double[][] predScores = new double[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			predScores[i] = new double[answerFlags[i].length];
			Arrays.fill(predScores[i], -2.0);
		}
		for (int i = 0; i < samples.size(); i++) {
			QASample sample = samples.get(i);
			int qid = sample.questionId;
			predScores[qid][sample.answerHead] =
					Linear.predict(model, features[i]);
			evalQIds.add(qid);
		}
		System.out.println(String.format("Evaluating %d questions.",
				evalQIds.size()));
		
		int numMatchedQuestions = 0;
		for (int qid = 0; qid < numQuestions; qid++) {
			if (!evalQIds.contains(qid)) {
				continue;
			}
			QAPair qa = questions.get(qid);
			int bestIdx = (qa.propHead > 0 ? qa.propHead - 1 : qa.propHead + 1);
			for (int i = 0; i < predScores[qid].length; i++) {
				if (predScores[qid][i] > predScores[qid][bestIdx]) {
					bestIdx = i;
				}
			}
			int matched = AnswerIdEvaluator.evaluateAccuracy(
					bestIdx,
					answerFlags[qid],
					answerHeads[qid],
					evalPrm);
			numMatchedQuestions += matched;
/*			
			if (samples.size() == 141324 && i % 37 == 1) {
				QAPair qa = questions.get(i);
				System.out.println(qa.sentence.getTokensString());
				System.out.print(qa.getQuestionString() + "\t" +
								 qa.getQuestionLabel() + "\t" +
								 qa.getAnswerString());
				int length = qa.sentence.length;
				System.out.print("\nAFLG:\t");
				for (int j = 0; j < length; j++) {
					System.out.print(String.format("%s(%d)\t", 
							qa.sentence.getTokenString(j), answerFlags[i][j]));
				}
				System.out.print("\nAHED:\t");
				for (int j = 0; j < length; j++) {
					System.out.print(String.format("%s(%d)\t", 
							qa.sentence.getTokenString(j), answerHeads[i][j]));
				}
				System.out.print("\nPSCR:\t");
				for (int j = 0; j < length; j++) {
					System.out.print(String.format("%s(%.3f)\t", 
							qa.sentence.getTokenString(j), predScores[i][j]));
				}
				System.out.println("\nmatched:" + matched);
			}
*/
		}
		return 1.0 * numMatchedQuestions /evalQIds.size();
	}
	
	private double crossValidate(AnswerIdDataset ds, int cvFolds,
			LiblinearHyperParameters prm) {
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();		
		for (int i = 0; i < ds.getQuestions().size(); i++) {
			shuffledIds.add(i);
		}
		Collections.shuffle(shuffledIds, new Random(randomSeed));
		int sampleSize = shuffledIds.size();
		int foldSize = sampleSize / cvFolds;
	
		double avgAcc = .0;
		for (int c = 0; c < cvFolds; c++) {
			System.out.println("cv: " + c);
			
			HashSet<Integer> trnQIds = new HashSet<Integer>();
			HashSet<Integer> valQIds = new HashSet<Integer>();
			ArrayList<QASample> trnSamples = new ArrayList<QASample>();
			ArrayList<QASample> valSamples = new ArrayList<QASample>();
			
			for (int j = 0; j < sampleSize; j++) {
				int qid = shuffledIds.get(j);
				if (j < foldSize * c || j >= foldSize * (c + 1)) {
					trnQIds.add(qid);
				} else {
					valQIds.add(qid);
				}
			}
			System.out.println(String.format(
					"%d questions in training, %d in validation",
					trnQIds.size(), valQIds.size()));
			

			for (int j = 0; j < ds.getSamples().size(); j++) {
				QASample sample = ds.getSamples().get(j);
				if (trnQIds.contains(sample.questionId)) {
					trnSamples.add(sample);
				} else {
					valSamples.add(sample);
				}
			}
			Feature[][] trnFeats = new Feature[trnSamples.size()][];		
			Feature[][] valFeats = new Feature[valSamples.size()][];
			double[] trnLabels = new double[trnSamples.size()];
			double[] valLabels = new double[valSamples.size()];
			
			int trnCnt = 0, valCnt = 0;
			
			for (int j = 0; j < ds.getSamples().size(); j++) {
				QASample sample = ds.getSamples().get(j);
				int qid = sample.questionId;
				if (trnQIds.contains(qid)) {
					trnFeats[trnCnt] = ds.getFeatures()[j];
					trnLabels[trnCnt++] = ds.getLabels()[j];
				} else {
					valFeats[valCnt] = ds.getFeatures()[j];
					valLabels[valCnt++] = ds.getLabels()[j];
				}
			}
			System.out.println(String.format(
					"%d samples in training, %d in validation",
					trnCnt, valCnt));
			
			Model model = train(trnFeats, trnLabels,
					featureExtractor.numFeatures(), prm);
			double trnAcc = predictAndEvaluate(
					trnSamples, trnFeats, ds, model);
			double valAcc = predictAndEvaluate(
					valSamples, valFeats, ds, model);
			avgAcc += valAcc;
			System.out.println(trnAcc);
			System.out.println(valAcc);
		}
		return avgAcc / cvFolds;
	}
	
	public static void main(String[] args) {
		BaselineQAExperiment exp = new BaselineQAExperiment(
				true /* generate sampels */);
		
		ArrayList<LiblinearHyperParameters> cvPrms =
				new ArrayList<LiblinearHyperParameters>();
		
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
	//	cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
	//	cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		
		LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		exp.trainAndPredict(bestPar);
	}
}
