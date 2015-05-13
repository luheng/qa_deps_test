package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import config.DataConfig;
import config.QuestionIdConfig;
import util.StringUtils;
import annotation.QuestionEncoder;
import learning.KBestParseRetriever;
import learning.QASample;
import learning.QuestionIdDataset;
import learning.QuestionIdFeatureExtractor;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import evaluation.F1Metric;
import experiments.LiblinearHyperParameters;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class BaselineQuestionIdExperiment {
	private QuestionIdConfig config;
	
	private Corpus baseCorpus; 
	private QuestionIdFeatureExtractor featureExtractor;
	private QuestionGenerator qgen;
	
	CountDictionary slotDict, tempDict;
	
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	public BaselineQuestionIdExperiment() throws IOException {
		config = new QuestionIdConfig();
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new HashMap<String, QuestionIdDataset>();
		
		// ********** Load QA Data ********************
		if (config.trainWithWiki) {
			trainSet = new QuestionIdDataset(baseCorpus, "wiki1-train");			
			for (String ds : new String[] {"wiki1-dev", "prop-dev"}) {
				testSets.put(ds, new QuestionIdDataset(baseCorpus, ds));
			}
		} else {
			trainSet = new QuestionIdDataset(baseCorpus, "prop-train");
			for (String ds : new String[] {"prop-dev", "wiki1-dev"}) {
				testSets.put(ds, new QuestionIdDataset(baseCorpus, ds));
			}
		}
		trainSet.loadData(DataConfig.getDataset(trainSet.datasetName));
		for (String ds : testSets.keySet()) {
			testSets.get(ds).loadData(DataConfig.getDataset(ds));
		}
		
		slotDict = new CountDictionary();
		tempDict = new CountDictionary();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				QuestionEncoder.encode(
						sent.sentence, 
						propHead,
						sent.qaLists.get(propHead),
						slotDict,
						tempDict);
			}
		}
		slotDict = new CountDictionary(slotDict, config.minQuestionLabelFreq);
		slotDict.prettyPrint();
		tempDict.prettyPrint();
		
		// *********** Generate training/test samples **********
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper = new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, slotDict);
			for (QuestionIdDataset ds : testSets.values()) {
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
				for (QuestionIdDataset testSet : testSets.values()) {
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
     			for (QuestionIdDataset testSet : testSets.values()) {
					testSet.loadSamples(getSampleFileName(testSet));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}		
	
		// ********** Extract features ***************
		featureExtractor = new QuestionIdFeatureExtractor(
				baseCorpus,
				config.kBest,
				config.minFeatureFreq,
				config.useLexicalFeatures,
				config.useDependencyFeatures);
		featureExtractor.extractFeatures(trainSet.samples);
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
			double res = crossValidate(trainSet, config.cvFolds, prm);
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
				trainSet.features,
				trainSet.labels,
				numFeatures, prm);
		
		qgen = new QuestionGenerator(baseCorpus, slotDict, tempDict);		
		for (QuestionIdDataset ds : testSets.values()) {
			if (ds.datasetName.contains("dev")) {
				generateQuestions(ds.samples, ds.features, ds, model);
			}
		}
		
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
		return predictAndEvaluate(ds.samples, ds.features, ds, model,
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
			int pred = prob[0] > config.evalThreshold ? 1 : -1;
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
	
	private void generateQuestions(
			ArrayList<QASample> samples,
			Feature[][] features,
			QuestionIdDataset ds,
			Model model) {
		HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results =
			new HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>>();
		
		for (int i = 0; i < samples.size(); i++) {
			QASample sample = samples.get(i);
			double[] prob = new double[2];
			Linear.predictProbability(model, features[i], prob);
			if (prob[0] < config.evalThreshold) {
				continue;
			}
			int sentId = sample.sentenceId;
			int propHead = sample.propHead;
			if (!results.containsKey(sentId)) {
				results.put(sentId, new HashMap<Integer, TIntDoubleHashMap>());
			}
			if (!results.get(sentId).containsKey(propHead)) {
				results.get(sentId).put(propHead, new TIntDoubleHashMap());
			}
			results.get(sentId).get(propHead).put(sample.questionLabelId,
					prob[0]);
			
		}
		for (AnnotatedSentence annotSent : ds.sentences) {
			int sentId = annotSent.sentence.sentenceID;
			if (!results.containsKey(sentId)) {
				continue;
			}
			
			for (int propHead : results.get(sentId).keySet()) {
				Sentence sent = ds.getSentence(sentId);
				ArrayList<String[]> questions = qgen.generateQuestions2(
						sent, propHead, results);
				if (questions == null) {
					continue;
				}
				System.out.println(sent.getTokensString());
				System.out.println(sent.getTokenString(propHead));
				System.out.println("=========== annotated ==============");
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					System.out.println(qa.getQuestionString() + "\t" + qa.getAnswerString());
				}
				System.out.println("=========== generated ==============");
				for (String[] question : questions) {
					System.out.println(StringUtils.join("\t", question));
				}
				System.out.println();
			}
		}
	}
	
	private double crossValidate(QuestionIdDataset ds, int cvFolds,
			LiblinearHyperParameters prm) {
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();		
		for (int i = 0; i < ds.questions.size(); i++) {
			shuffledIds.add(i);
		}
		Collections.shuffle(shuffledIds, new Random(config.randomSeed));
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
					trnSamples.add(ds.samples.get(qid));
				} else {
					valSamples.add(ds.samples.get(qid));
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
					trnFeats[trnCnt] = ds.features[qid];
					trnLabels[trnCnt++] = ds.labels[qid];
				} else {
					valFeats[valCnt] = ds.features[qid];
					valLabels[valCnt++] = ds.labels[qid];
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
