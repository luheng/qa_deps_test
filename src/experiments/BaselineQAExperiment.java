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
	
	private String oodFilePath = "data/odesk_wiki1.qa";
	
	private String trainSamplesPath = "odesk_s1250_20best.train.qaSamples";
//	private String testSamplesPath = "odesk_s1250_20best.test.qaSamples";
	
	private String featureOutputPath = "feature_weights.tsv";
	
	private final int randomSeed = 12345;
	private int cvFolds = 5, minFeatureFreq = 5, kBest = 20;
	
	private EvaluationType evalType = EvaluationType.BinaryHead;
	
	private double evalThreshold = 0;
	private int evalKBest = 1;
	
	private Corpus baseCorpus; 
	private AnswerIdFeatureExtractor featureExtractor;
	private AnswerIdEvaluationParameters evalPrm;
	
	private AnswerIdDataset trainSets;
	private HashMap<String, AnswerIdDataset> testSets;
	
	
	public BaselineQAExperiment(boolean generateSamples) {
		baseCorpus = new Corpus("qa-exp-corpus");
		trainSets = new AnswerIdDataset(baseCorpus);
		testSets = new HashMap<String, AnswerIdDataset>();
		testSets.put("prop-test", new AnswerIdDataset(baseCorpus));
		testSets.put("wiki1", new AnswerIdDataset(baseCorpus));
		
		//tests = new AnswerIdDataset(baseCorpus);
		evalPrm = new AnswerIdEvaluationParameters(evalType, evalKBest,
				evalThreshold);
	
		// ********** Load QA Data ********************
		try {
			trainSets.loadData(trainFilePath);
			testSets.get("prop-test").loadData(testFilePath);
			testSets.get("wiki1").loadData(oodFilePath);
		} catch (IOException e) {
			e.printStackTrace();	
		}
		
		// *********** Generate training/test samples **********
		if (generateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trainSets.generateSamples(syntaxHelper);
			for (AnswerIdDataset ds : testSets.values()) {
				ds.generateSamples(syntaxHelper);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(trainSamplesPath));
				ostream.writeObject(trainSets.getSamples());
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
				trainSets.loadSamples(trainSamplesPath);
				for (String dsName : testSets.keySet()) {
					testSets.get(dsName).loadSamples(dsName + ".qaSamples");
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		
		// ********** Extract features ***************
		ArrayList<QASample> allSamples = new ArrayList<QASample>();
		allSamples.addAll(trainSets.getSamples());
		for (AnswerIdDataset ds : testSets.values()) {
			allSamples.addAll(ds.getSamples());
		}
		featureExtractor = new AnswerIdFeatureExtractor(baseCorpus, kBest,
				minFeatureFreq);
		featureExtractor.extractFeatures(allSamples);
		trainSets.extractFeaturesAndLabels(featureExtractor);
		for (AnswerIdDataset ds : testSets.values()) {
			ds.extractFeaturesAndLabels(featureExtractor);
		}
	}
	
	public LiblinearHyperParameters runCrossValidation(
			ArrayList<LiblinearHyperParameters> cvPrms) {
		ArrayList<double[]> cvResults = new ArrayList<double[]>();
		double bestF1 = .0;
		LiblinearHyperParameters bestPrm = null;
		
		for (LiblinearHyperParameters prm : cvPrms) {
			System.out.println(prm.toString());
			double[] res = crossValidate(trainSets, cvFolds, prm);
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
				trainSets.getFeatures(),
				trainSets.getLabels(),
				numFeatures, prm);
		F1Metric f1 = predictAndEvaluate(trainSets, model);
		System.out.println("Training accuracy:\t" + f1.toString());
		for (String dsName : testSets.keySet()) {
			AnswerIdDataset ds = testSets.get(dsName);
			f1 = predictAndEvaluate(ds, model);
			F1Metric oldF1 = predictAndEvaluateOld(ds.getFeatures(), 
					ds.getLabels(), model);
			System.out.println(String.format("Testing accuracy on %s: %s",
					dsName, f1.toString()));		
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
	
	private F1Metric predictAndEvaluate(AnswerIdDataset ds, Model model) {
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
	
	private F1Metric predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			AnswerIdDataset ds,
			Model model) {
		int[][] answerFlags = ds.getAnswerFlags();
		int[][] answerHeads = ds.getAnswerHeads();
		ArrayList<QAPair> questions = ds.getQuestions();
		
		F1Metric totalF1 = new F1Metric();
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
		for (int i = 0; i < numQuestions; i++) {
			if (!evalQIds.contains(i)) {
				continue;
			}
			
			F1Metric f1 = AnswerIdEvaluator.evaluate(predScores[i],
					answerFlags[i],
					answerHeads[i],
					evalPrm);
			totalF1.add(f1);
			
		/*	if (samples.size() == 54175) {
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
					System.out.print(String.format("%s(%.0f)\t", 
							qa.sentence.getTokenString(j), predScores[i][j]));
				}
				System.out.println("\n" + f1.toString());
			}
		*/
		}
		return totalF1;
	}
	
	private double[] crossValidate(AnswerIdDataset ds, int cvFolds,
			LiblinearHyperParameters prm) {
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();		
		for (int i = 0; i < ds.getQuestions().size(); i++) {
			shuffledIds.add(i);
		}
		Collections.shuffle(shuffledIds, new Random(randomSeed));
		int sampleSize = shuffledIds.size();
		int foldSize = sampleSize / cvFolds;
	
		double avgPrec = .0, avgRecall = .0, avgF1 = .0;
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
			F1Metric trnF1 = predictAndEvaluate(
					trnSamples, trnFeats, ds, model);
			F1Metric valF1 = predictAndEvaluate(
					valSamples, valFeats, ds, model);
			avgPrec += valF1.precision();
			avgRecall += valF1.recall();
			avgF1 += valF1.f1();
			System.out.println(trnF1.toString());
			System.out.println(valF1.toString());
		}
		return new double[] {avgPrec / cvFolds,
							 avgRecall / cvFolds,
							 avgF1 / cvFolds};
	}
	
	public static void main(String[] args) {
		BaselineQAExperiment exp = new BaselineQAExperiment(
				false /* generate sampels */);
		
		ArrayList<LiblinearHyperParameters> cvPrms =
				new ArrayList<LiblinearHyperParameters>();
		
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L1R_LR, 0.1, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVC, 0.1, 1e-3));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVR, 0.1, 1e-3));
		
		LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		exp.trainAndPredict(bestPar);
	}
}
