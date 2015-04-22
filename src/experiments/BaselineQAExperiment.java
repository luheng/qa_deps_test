package experiments;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
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
import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;
import data.UniversalPostagMap;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
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
	
	private String trainSamplesPath = "odesk_s1250_20best.train.qaSamples";
	private String testSamplesPath = "odesk_s1250_20best.test.qaSamples";
	
	private final int randomSeed = 12345;
	private int cvFolds = 5, minFeatureFreq = 5, kBest = 20;
	
	private EvaluationType evalType = EvaluationType.BinarySpan;
	private double evalThreshold = 0;
	private int evalKBest = 1;
	
	private Corpus baseCorpus; 
	private AnswerIdFeatureExtractor featureExtractor;
	private AnswerIdEvaluationParameters evalPrm;
	
	private AnswerIdDataset trains, tests;
	
	
	public BaselineQAExperiment(boolean generateSamples) {
		baseCorpus = new Corpus("qa-exp-corpus");
		trains = new AnswerIdDataset(baseCorpus);
		tests = new AnswerIdDataset(baseCorpus);
		evalPrm = new AnswerIdEvaluationParameters(evalType, evalKBest,
				evalThreshold);
	
		// ********** Load QA Data ********************
		try {
			trains.loadData(trainFilePath);
			tests.loadData(testFilePath);
		} catch (IOException e) {
			e.printStackTrace();	
		}
		
		// *********** Generate training/test samples **********
		if (generateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trains.generateSamples(syntaxHelper);
			tests.generateSamples(syntaxHelper);
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(trainSamplesPath));
				ostream.writeObject(trains.getSamples());
				ostream.flush();
				ostream.close();
				ostream = new ObjectOutputStream(
						new FileOutputStream(testSamplesPath));
				ostream.writeObject(trains.getSamples());
				ostream.flush();
				ostream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				trains.loadSamples(trainSamplesPath);
				tests.loadSamples(testSamplesPath);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		
		// ********** Extract features ***************
		ArrayList<QASample> allSamples = new ArrayList<QASample>();
		allSamples.addAll(trains.getSamples());
		allSamples.addAll(tests.getSamples());
		featureExtractor = new AnswerIdFeatureExtractor(baseCorpus, kBest,
				minFeatureFreq);
		featureExtractor.extractFeatures(allSamples);
		trains.extractFeaturesAndLabels(featureExtractor);
		tests.extractFeaturesAndLabels(featureExtractor);
	}
	
	public LiblinearHyperParameters runCrossValidation(
			ArrayList<LiblinearHyperParameters> cvPrms) {
		ArrayList<double[]> cvResults = new ArrayList<double[]>();
		double bestF1 = .0;
		LiblinearHyperParameters bestPrm = null;
		
		for (LiblinearHyperParameters prm : cvPrms) {
			double[] res = crossValidate(trains, cvFolds, prm);
			cvResults.add(res);
		}
		for (int i = 0; i < cvResults.size(); i++) {
			double[] res = cvResults.get(i);
			System.out.println(String.format("%s\t%.3f\t%.3f\t%.3f\n",
					cvPrms.get(i).toString(), res[0], res[1], res[2]));
			if (res[2] > bestF1) {
				bestF1 = res[2];
				bestPrm = cvPrms.get(i);
			}
		}
		return bestPrm;
	}
	
	public void trainAndPredict(LiblinearHyperParameters prm) {
		int numFeatures = featureExtractor.numFeatures();
		Model model = train(
				trains.getFeatures(),
				trains.getLabels(),
				numFeatures, prm);
		
		F1Metric f1 = predictAndEvaluate(trains, model);
		System.out.println("Training accuracy:\t" + f1.toString());
		f1 = predictAndEvaluate(tests, model);
		System.out.println("Testing accuracy:\t" + f1.toString());		
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
	
	private F1Metric predictAndEvaluate(AnswerIdDataset ds, Model model) {
		F1Metric totalF1 = new F1Metric();
		int numQuestions = ds.getQuestions().size();
		double[][] predScores = new double[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			predScores[i] = new double[ds.getQuestions().get(i).sentence.length];
			Arrays.fill(predScores, -1.0);
		}
		for (int i = 0; i < ds.getSamples().size(); i++) {
			QASample sample = ds.getSamples().get(i);
			predScores[sample.questionId][sample.answerHead] =
					Linear.predict(model, ds.getFeatures()[i]);
		}
		for (int i = 0; i < numQuestions; i++) {
			F1Metric f1 = AnswerIdEvaluator.evaluate(predScores[i],
					ds.getAnswerFlags()[i],
					ds.getAnswerHeads()[i],
					evalPrm);
			totalF1.add(f1);
		}
		return totalF1;
	}
	
	private double[] crossValidate(AnswerIdDataset ds, int cvFolds,
			LiblinearHyperParameters prm) {
		// Split by questions
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();
		ArrayList<ArrayList<Integer>> sampleIds =
				new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < ds.getQuestions().size(); i++) {
			shuffledIds.add(i);
			sampleIds.add(new ArrayList<Integer>());
		}
		Collections.shuffle(shuffledIds, new Random(randomSeed));
		int sampleSize = shuffledIds.size();
		int foldSize = sampleSize / cvFolds;
	
		double avgPrec = .0, avgRecall = .0, avgF1 = .0;
		for (int i = 0; i < cvFolds; i++) {
			HashSet<Integer> trainQIds = new HashSet<Integer>();
			HashSet<Integer> valQIds = new HashSet<Integer>();
			
			for (int j = 0; j < sampleSize; i++) {
				int qid = shuffledIds.get(j);
				sampleIds.get(qid).clear();
				if (j < foldSize * i || j >= foldSize * (i + 1)) {
					trainQIds.add(qid);
				} else {
					valQIds.add(qid);
				}
			}
			int numAll = ds.getSamples().size(),
				trnCnt = 0, valCnt = 0;
		
			for (int j = 0; j < ds.getSamples().size(); j++) {
				QASample sample = ds.getSamples().get(j);
				if (trainQIds.contains(sample.questionId)) {
					trnCnt ++;
				}
			}
			Feature[][] trnFeats = new Feature[trnCnt][];		
			Feature[][] valFeats = new Feature[numAll - trnCnt][];
			double[] trnLabels = new double[trnCnt];
			double[] valLabels = new double[numAll - trnCnt];
			int[] trnHeads = new int[trnCnt];
			int[] valHeads = new int[numAll - trnCnt];
			
			trnCnt = 0;
			for (int j = 0; j < ds.getSamples().size(); j++) {
				QASample sample = ds.getSamples().get(j);
				int qid = sample.questionId;
				if (trainQIds.contains(qid)) {
					sampleIds.get(qid).add(trnCnt);
					trnHeads[trnCnt] = sample.answerHead;
					trnFeats[trnCnt] = ds.getFeatures()[j];
					trnLabels[trnCnt++] = ds.getLabels()[j];
				} else {
					sampleIds.get(qid).add(valCnt);
					valHeads[valCnt] = sample.answerHead;
					valFeats[valCnt] = ds.getFeatures()[j];
					valLabels[valCnt++] = ds.getLabels()[j];
				}
			}
			Model model = train(trnFeats, trnLabels,
					featureExtractor.numFeatures(), prm);
			F1Metric cvF1 = new F1Metric();
			// Predict and eval
			for (int qid : valQIds) {
				QAPair qa = ds.getQuestions().get(qid);
				double[] predScores = new double[qa.sentence.length];
				Arrays.fill(predScores, -1.0);
				for (int sid : sampleIds.get(qid)) {
					predScores[valHeads[sid]] =
						Linear.predict(model, valFeats[i]);
				}
				F1Metric f1 = AnswerIdEvaluator.evaluate(
						predScores,
						ds.getAnswerFlags()[qid],
						ds.getAnswerHeads()[qid],
						evalPrm);
				cvF1.add(f1);
			}
			avgPrec += cvF1.precision();
			avgRecall += cvF1.recall();
			avgF1 += cvF1.f1();
		}
		return new double[] {avgPrec / cvFolds,
							 avgRecall / cvFolds,
							 avgF1 / cvFolds};
	}
	
	public static void main(String[] args) {
		BaselineQAExperiment exp = new BaselineQAExperiment(
				true /* generate sampels */);
		
		ArrayList<LiblinearHyperParameters> cvPrms =
				new ArrayList<LiblinearHyperParameters>();
		
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		//cvPrms.add(new LiblinearHyperParameters(SolverType.L1R_LR, 0.1, 1e-2));
		//cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVC, 0.1, 1e-3));
		//cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVR, 0.1, 1e-3));
		
		LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		exp.trainAndPredict(bestPar);
	}
}
