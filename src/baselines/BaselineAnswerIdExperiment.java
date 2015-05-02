package baselines;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import config.ExperimentDataConfig;
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
import evaluation.AnswerIdEvaluator;
import evaluation.F1Metric;
import experiments.LiblinearHyperParameters;

public class BaselineAnswerIdExperiment {
	private String featureOutputPath = "feature_weights.tsv";
	
	private final int randomSeed = 12345;
	private final int cvFolds = 5,
					  minFeatureFreq = 3,
					  kBest = 20,
					  featureKBest = 20;
	
	private boolean regenerateSamples = false;
	private boolean trainWithWiki = false;
	private boolean useSpanBasedSamples = false;
	private boolean useLexicalFeatures = true;
	private boolean useDependencyFeatures = true;
	
	private Corpus baseCorpus; 
	private AnswerIdFeatureExtractor featureExtractor;
	
	private AnswerIdDataset trainSet;
	private HashMap<String, AnswerIdDataset> testSets;
	
	private String getSampleFileName(AnswerIdDataset ds) {
		if (useSpanBasedSamples) {
			return ds.datasetName + ".sp.k" + kBest + ".smp";
		} else {
			return ds.datasetName + ".k" + kBest + ".smp";
		}
	}
	
	public BaselineAnswerIdExperiment() throws IOException {
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new HashMap<String, AnswerIdDataset>();
		// ********** Load QA Data ********************
		if (trainWithWiki) {
			trainSet = new AnswerIdDataset(baseCorpus, "wiki1-train");
			testSets.put("prop-train", new AnswerIdDataset(baseCorpus, "prop-train"));
			
			trainSet.loadData(ExperimentDataConfig.get("wikiQATrainFilename"));
			testSets.get("prop-train").loadData(ExperimentDataConfig.get("propbankQATrainFilename"));
		} else {
			trainSet = new AnswerIdDataset(baseCorpus, "prop-train");
			testSets.put("wiki1-train", new AnswerIdDataset(baseCorpus, "wiki1-train"));
			
			trainSet.loadData(ExperimentDataConfig.get("propbankQATrainFilename"));
			testSets.get("wiki1-train").loadData(ExperimentDataConfig.get("wikiQATrainFilename"));
		}
		
		// *********** Generate training/test samples **********
		String dataPath = ExperimentDataConfig.get("tempOutputDatapath");
		if (regenerateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trainSet.generateSamples(syntaxHelper, useSpanBasedSamples);
			for (AnswerIdDataset ds : testSets.values()) {
				ds.generateSamples(syntaxHelper, useSpanBasedSamples);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(
								dataPath + getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.getSamples());
				ostream.flush();
				ostream.close();
				for (AnswerIdDataset testSet : testSets.values()) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(
									dataPath + getSampleFileName(testSet)));
					ostream.writeObject(testSet.getSamples());
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
     			trainSet.loadSamples(dataPath + getSampleFileName(trainSet));
     			for (AnswerIdDataset testSet : testSets.values()) {
					testSet.loadSamples(dataPath + getSampleFileName(testSet));
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
		featureExtractor = new AnswerIdFeatureExtractor(
				baseCorpus,
				featureKBest,
				minFeatureFreq,
				useLexicalFeatures,
				useDependencyFeatures);
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
		double[] accuracy = predictAndEvaluate(trainSet, model, "");
		System.out.println(String.format("Training accuracy on %s:\t%.4f\t%.4f",
				trainSet.datasetName, accuracy[0], accuracy[1]));
		for (AnswerIdDataset ds : testSets.values()) {
			accuracy = predictAndEvaluate(ds, model,
					String.format(ds.datasetName + ".debug.txt"));
			System.out.println(String.format(
					"Testing accuracy on %s:\t%.4f\t%.4f",
						ds.datasetName, accuracy[0], accuracy[1]));
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
	
	private double[] predictAndEvaluate(
			AnswerIdDataset ds,
			Model model,
			String debugFilePath) {
		return predictAndEvaluate(
				ds.getSamples(),
				ds.getFeatures(),
				ds,
				model,
				debugFilePath);
	}
	
	@SuppressWarnings("unused")
	@Deprecated
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
	
	private double[] predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			AnswerIdDataset ds,
			Model model,
			String debugFilePath) {
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
			double[] pred = new double[2];
			Linear.predictProbability(model, features[i], pred);
			predScores[qid][sample.answerWordPosition] = pred[0];
			evalQIds.add(qid);
		}
		//System.out.println(String.format("Evaluating %d questions.",
		//		evalQIds.size()));
		
		int numMatched = 0, numContained = 0;
		double avgMatchedWords = .0;
		double allNegBaseline = .0;
		BufferedWriter writer = null;
		if (!debugFilePath.isEmpty()) {
			try {
				writer = new BufferedWriter(
						new FileWriter(new File(debugFilePath)));
			} catch (IOException e) {
			}
		}
		
		for (int qid = 0; qid < numQuestions; qid++) {
			if (!evalQIds.contains(qid)) {
				continue;
			}
			QAPair qa = questions.get(qid);
			int length = predScores[qid].length;
			int bestIdx = -1;
			if (useSpanBasedSamples) {
				double matched = AnswerIdEvaluator.evaluateAccuracy(
						predScores[qid],
						answerFlags[qid],
						0.5 /* threshold */);
				avgMatchedWords += matched;
				int numGold = 0;
				for (int flag : answerFlags[qid]) {
					numGold += (flag > 0 ? 1 : 0);
				}
				allNegBaseline += (1.0 - 1.0 * numGold / answerFlags[qid].length);
			} else {
				for (int i = 0; i < length; i++) {
					if (predScores[qid][i] < 1e-6) {
						continue;
					}
					if (bestIdx < 0 ||
						predScores[qid][i] > predScores[qid][bestIdx]) {
						bestIdx = i;
					}
				}
				if (bestIdx > -1 && answerFlags[qid][bestIdx] > 0) {
					numContained ++;
				}
				if (bestIdx > -1 && answerHeads[qid][bestIdx] > 0) {
					numMatched ++;
				}
			}
			if (writer != null) {
				try {
					writer.append(qa.sentence.getTokensString() + "\n");
					writer.append(qa.getQuestionString() + "\t" +
							 qa.getQuestionLabel() + "\t" +
							 qa.getAnswerString() + "\t" +
							 (bestIdx < 0 ? "-" :
								 qa.sentence.getTokenString(bestIdx)) + "\n");
					for (int j = 0; j < length; j++) {
						if (predScores[qid][j] > 0) {
							writer.append(String.format("%s(%.3f)\t", 
								qa.sentence.getTokenString(j), predScores[qid][j]));
						}
					}
					writer.append("\n\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (useSpanBasedSamples) {
			System.out.println("All negative baseline:\t" +
					allNegBaseline / evalQIds.size());
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		double[] result = new double[2];
		if (useSpanBasedSamples) {
			result[0] = avgMatchedWords / evalQIds.size();
		} else {
			result[0] = 1.0 * numMatched /evalQIds.size();
			result[1] = 1.0 * numContained /evalQIds.size();
		}
		return result;				
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
			double[] trnAcc = predictAndEvaluate(
					trnSamples, trnFeats, ds, model, "");
			double[] valAcc = predictAndEvaluate(
					valSamples, valFeats, ds, model, "");
			avgAcc += valAcc[0];
			System.out.println(trnAcc);
			System.out.println(valAcc);
		}
		return avgAcc / cvFolds;
	}
	
	public static void main(String[] args) {
		BaselineAnswerIdExperiment exp = null;
		try {
			exp = new BaselineAnswerIdExperiment();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		ArrayList<LiblinearHyperParameters> cvPrms =
				new ArrayList<LiblinearHyperParameters>();
		
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L1R_LR, 1.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L1R_LR, 10.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L1R_LR, 0.1, 1e-2));
		
		LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		exp.trainAndPredict(bestPar);
	}
}
