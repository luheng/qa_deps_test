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

import util.StringUtils;
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
	private String oodTrainFilePath = "data/odesk_wiki1.train.qa";
	//private String testFilePath = "data/odesk_s1250.test.qa";
	//private String oodTestFilePath = "data/odesk_wiki1.test.qa";
	
	private String featureOutputPath = "feature_weights.tsv";
	
	private final int randomSeed = 12345;
	private final int cvFolds = 5;
	private final int minFeatureFreq = 3;
	private final int minLabelFreq = 5;
	private final int kBest = 1; // 20
	
	
	private boolean regenerateSamples = true;
	private boolean trainWithWiki = false;
	private boolean useLexicalFeatures = true;
	private boolean useDependencyFeatures = true;
	
	private double evalThreshold = 0.4999;
	
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
		
		// ********** Load QA Data ********************
		if (trainWithWiki) {
			trainSet = new QuestionIdDataset(baseCorpus, "wiki1-train");			
			testSets.put("prop-train", new QuestionIdDataset(baseCorpus, "prop-train"));
			// testSets.put("wiki1-test", new QuestionIdDataset(baseCorpus, "wiki1-test"));
			// testSets.put("prop-test", new QuestionIdDataset(baseCorpus, "prop-test"));
			
			trainSet.loadData(oodTrainFilePath);
			testSets.get("prop-train").loadData(trainFilePath);
			//	testSets.get("wiki1-test").loadData(oodTestFilePath);
			//	testSets.get("prop-test").loadData(testFilePath);
		} else {
			trainSet = new QuestionIdDataset(baseCorpus, "prop-train");	
			testSets.put("wiki1-train", new QuestionIdDataset(baseCorpus, "wiki1-train"));
			// testSets.put("prop-test", new QuestionIdDataset(baseCorpus, "prop-test"));
			// testSets.put("wiki1-test", new QuestionIdDataset(baseCorpus, "wiki1-test"));
			
			trainSet.loadData(trainFilePath);
			testSets.get("wiki1-train").loadData(oodTrainFilePath);
			//	testSets.get("wiki1-test").loadData(oodTestFilePath);
			//	testSets.get("prop-test").loadData(testFilePath);
		}

		// Each QA is associcated with a set of question labels, each label has different granularity.
		CountDictionary tempQDict = new CountDictionary();
		for (QAPair qa : trainSet.getQuestions()) {
			String[] qlabels = QuestionEncoder.getMultiQuestionLabels(qa.questionWords, qa);
			for (String qlabel : qlabels) {
				tempQDict.addString(qlabel);
			}
		}
		System.out.println("Saw " + tempQDict.size() + " distinct question labels.");
		CountDictionary qdict = new CountDictionary(tempQDict, minLabelFreq);
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
		int numKeptLabels = 0;
		for (int i = 0; i < qdict.size(); i++) {
			if (qdict.getCount(i) >= minLabelFreq) {
				numKeptLabels ++;
			}
		}
		System.out.println("Number of labels:\t" + numKeptLabels);
		
		
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
		System.out.println(String.format("Training accuracy on %s:\t%s",
				trainSet.datasetName,
				StringUtils.doubleArrayToString("\t", accuracy)));
		for (QuestionIdDataset ds : testSets.values()) {
			accuracy = predictAndEvaluate(ds, model, "");
			System.out.println(String.format("Testing accuracy on %s:\t%s",
					ds.datasetName,
					StringUtils.doubleArrayToString("\t", accuracy)));
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
	
	private double[] predictAndEvaluate(QuestionIdDataset ds, Model model,
			String debugFilePath) {
		return predictAndEvaluate(ds.getSamples(), ds.getFeatures(), ds, model,
				debugFilePath);
	}
	
	/**
	 * 
	 * @param samples
	 * @param features
	 * @param ds
	 * @param model
	 * @param debugFilePath
	 * @return [accuracy, precision, recall, F1] 
	 */
	private double[] predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			QuestionIdDataset ds,
			Model model,
			String debugFilePath) {
		
		F1Metric f1 = new F1Metric();
		int numCorrect = 0;
		for (int i = 0; i < samples.size(); i++) {
			QASample sample = samples.get(i);
			double[] prob = new double[2];
			Linear.predictProbability(model, features[i], prob);
			int gold = sample.isPositiveSample ? 1 : -1;
			int pred = prob[0] > evalThreshold ? 1 : -1;
			f1.numGold += (gold > 0 ? 1 : 0);
			f1.numProposed += (pred > 0 ? 1 : 0);
			f1.numMatched += ((gold > 0 && pred > 0) ? 1 : 0);
			numCorrect += (gold == pred ? 1 : 0);
		}
		return new double[] {
				1.0 * numCorrect / samples.size(),
				f1.precision(), f1.recall(), f1.f1()}; 
		/*
		BufferedWriter writer = null;
		if (!debugFilePath.isEmpty()) {
			try {
				writer = new BufferedWriter(
						new FileWriter(new File(debugFilePath)));
			} catch (IOException e) {
			}
		}
		*/
	}
	
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
			ArrayList<QASample> trnSamples = new ArrayList<QASample>();
			ArrayList<QASample> valSamples = new ArrayList<QASample>();
			
			for (int j = 0; j < sampleSize; j++) {
				int qid = shuffledIds.get(j);
				if (j < foldSize * c || j >= foldSize * (c + 1)) {
					trnSamples.add(ds.getSamples().get(qid));
				} else {
					valSamples.add(ds.getSamples().get(qid));
				}
			}
			System.out.println(String.format(
					"%d questions in training, %d in validation",
					trnSamples.size(), valSamples.size()));
			
			Feature[][] trnFeats = new Feature[trnSamples.size()][];		
			Feature[][] valFeats = new Feature[valSamples.size()][];
			double[] trnLabels = new double[trnSamples.size()];
			double[] valLabels = new double[valSamples.size()];
			
			int trnCnt = 0, valCnt = 0;
			for (int j = 0; j < sampleSize; j++) {
				int qid = shuffledIds.get(j);
				if (j < foldSize * c || j >= foldSize * (c + 1)) {
					trnFeats[trnCnt] = ds.getFeatures()[qid];
					trnLabels[trnCnt++] = ds.getLabels()[qid];
				} else {
					valFeats[valCnt] = ds.getFeatures()[qid];
					valLabels[valCnt++] = ds.getLabels()[qid];
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
			avgAcc += valAcc[3];
			System.out.println(StringUtils.doubleArrayToString("\t", trnAcc));
			System.out.println(StringUtils.doubleArrayToString("\t", valAcc));
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
