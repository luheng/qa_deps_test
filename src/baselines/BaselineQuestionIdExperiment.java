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

import annotation.QuestionEncoder;
import learning.KBestParseRetriever;
import learning.QASample;
import learning.QuestionIdDataset;
import learning.QuestionIdFeatureExtractor;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import evaluation.F1Metric;
import experiments.LiblinearHyperParameters;

public class BaselineQuestionIdExperiment {

	private String trainFilePath = "data/odesk_s1250.train.qa";
	private String testFilePath = "data/odesk_s1250.test.qa";	
	private String oodTrainFilePath = "data/odesk_wiki1.train.qa";
	private String oodTestFilePath = "data/odesk_wiki1.test.qa";
	
	private String featureOutputPath = "feature_weights.tsv";
	
	private final int randomSeed = 12345;
	private final int cvFolds = 5,
					  minFeatureFreq = 3,
					  kBest = 1;
	
	private boolean regenerateSamples = false;
	private boolean trainWithWiki = false;
	private boolean useLexicalFeatures = true;
	private boolean useDependencyFeatures = true;
	
	private Corpus baseCorpus; 
	private QuestionIdFeatureExtractor featureExtractor;
	
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + kBest + ".smp";
	}
	
	public BaselineQuestionIdExperiment() throws IOException {
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new HashMap<String, QuestionIdDataset>();
		CountDictionary qdict = new CountDictionary();
		
		// ********** Load QA Data ********************
		if (trainWithWiki) {
			trainSet = new QuestionIdDataset(baseCorpus, qdict, "wiki1-train");
			testSets.put("wiki1-test", new QuestionIdDataset(baseCorpus, qdict, "wiki1-test"));			
			testSets.put("prop-train", new QuestionIdDataset(baseCorpus, qdict, "prop-train"));
			testSets.put("prop-test", new QuestionIdDataset(baseCorpus, qdict, "prop-test"));
			
			trainSet.loadData(oodTrainFilePath);
			testSets.get("wiki1-test").loadData(oodTestFilePath);
			testSets.get("prop-train").loadData(trainFilePath);
			testSets.get("prop-test").loadData(testFilePath);
		} else {
			trainSet = new QuestionIdDataset(baseCorpus, qdict, "prop-train");
			testSets.put("prop-test", new QuestionIdDataset(baseCorpus, qdict, "prop-test"));	
			testSets.put("wiki1-train", new QuestionIdDataset(baseCorpus, qdict, "wiki1-train"));
			testSets.put("wiki1-test", new QuestionIdDataset(baseCorpus, qdict, "wiki1-test"));
			
			trainSet.loadData(trainFilePath);
			testSets.get("prop-test").loadData(testFilePath);
			testSets.get("wiki1-train").loadData(oodTrainFilePath);
			testSets.get("wiki1-test").loadData(oodTestFilePath);
		}

		// Each QA is associcated with a set of question labels, each label has different granularity. 
		for (QAPair qa : trainSet.getQuestions()) {
			String[] qlabels = QuestionEncoder.getMultiQuestionLabels(qa.questionWords, qa);
			for (String qlabel : qlabels) {
				qdict.addString(qlabel);
			}
		}
		System.out.println("Saw " + qdict.size() + " distinct question labels.");
		int numUnseenQuestionLabels = 0;
		for (QuestionIdDataset testSet : testSets.values()) {
			for (QAPair qa : testSet.getQuestions()) {
				String[] qlabels = QuestionEncoder.getMultiQuestionLabels(qa.questionWords, qa);
				for (String qlabel : qlabels) {
					int qid = qdict.lookupString(qlabel);
					if (qid < 0) {
						System.out.println("Unseen question labels:\t" + qlabel);
						numUnseenQuestionLabels ++;
					}
				}
			}
		}
		System.out.println("Number of unseen labels:\t" + numUnseenQuestionLabels);
		// *********** Generate training/test samples **********
		if (regenerateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(kBest);
			trainSet.generateSamples(syntaxHelper, qdict);
			for (QuestionIdDataset ds : testSets.values()) {
				ds.generateSamples(syntaxHelper, qdict);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.getSamples());
				ostream.flush();
				ostream.close();
				for (QuestionIdDataset testSet : testSets.values()) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(getSampleFileName(testSet)));
					ostream.writeObject(testSet.getSamples());
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
     			trainSet.loadSamples(getSampleFileName(trainSet));
     			for (QuestionIdDataset testSet : testSets.values()) {
					testSet.loadSamples(getSampleFileName(testSet));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}		
		sanityCheck1();

		// ********** Extract features ***************
		// TODO: question id feature extractor ..
		featureExtractor = new QuestionIdFeatureExtractor(
				baseCorpus,
				kBest,
				minFeatureFreq,
				useLexicalFeatures,
				useDependencyFeatures);
		featureExtractor.extractFeatures(trainSet.getSamples());
		trainSet.extractFeaturesAndLabels(featureExtractor);
		for (QuestionIdDataset ds : testSets.values()) {
			ds.extractFeaturesAndLabels(featureExtractor);
		}
	}
	
	public void sanityCheck1() {
		// 1. Different datasets should have disjoint sentence Ids.
		System.out.println("======= Sanity Check1: Sentence Overlap =======");
		int overlap = 0;
		HashSet<Integer> sids = trainSet.getSentenceIds();
		for (QuestionIdDataset ds : testSets.values()) {
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
		for (QuestionIdDataset ds : testSets.values()) {
			accuracy = predictAndEvaluate(ds, model);
//			F1Metric oldF1 = predictAndEvaluateOld(ds.getFeatures(),  ds.getLabels(), model);
			System.out.println(String.format("Testing accuracy on %s: %.4f",
					ds.datasetName, accuracy));		
//			System.out.println("Old f1:\t" + oldF1.toString());
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
	
	private double predictAndEvaluate(QuestionIdDataset ds, Model model) {
		return predictAndEvaluate(ds.getSamples(), ds.getFeatures(), ds, model);
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
	
	// TODO: do this ...
	private double predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			QuestionIdDataset ds,
			Model model) {
		return -1;
	}
	
	// TODO: do this ...
	private double crossValidate(QuestionIdDataset ds, int cvFolds,
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
		BaselineQuestionIdExperiment exp = null;
		try {
			exp = new BaselineQuestionIdExperiment();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		ArrayList<LiblinearHyperParameters> cvPrms =
				new ArrayList<LiblinearHyperParameters>();
		
		//cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
		//cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
		cvPrms.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		
		LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		exp.trainAndPredict(bestPar);
	}
}
