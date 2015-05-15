package baselines;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import config.AnswerIdConfig;
import config.DataConfig;
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
import evaluation.AnswerIdEvaluator;
import experiments.LiblinearHyperParameters;
import util.StringUtils;

public class BaselineAnswerIdExperiment {
	private AnswerIdConfig config;
	private Corpus baseCorpus; 
	private AnswerIdFeatureExtractor featureExtractor;
	
	private AnswerIdDataset trainSet;
	private ArrayList<AnswerIdDataset> testSets;
	
	private String getSampleFileName(AnswerIdDataset ds) {
		if (config.useSpanBasedSamples) {
			return ds.datasetName + ".sp.k" + config.kBest + ".smp";
		} else {
			return ds.datasetName + ".k" + config.kBest + ".smp";
		}
	}
	
	public BaselineAnswerIdExperiment(String answerIdConfigPath)
			throws IOException, ClassNotFoundException {
		config = answerIdConfigPath.isEmpty() ? new AnswerIdConfig() :
						new AnswerIdConfig(answerIdConfigPath);
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
		
		sanityCheck1();
		
		featureExtractor = new AnswerIdFeatureExtractor(
				baseCorpus,
				config.featureKBest,
				config.minFeatureFreq,
				config.useLexicalFeatures,
				config.useDependencyFeatures);
		featureExtractor.extractFeatures(trainSet.samples);
		trainSet.extractFeaturesAndLabels(featureExtractor);
		for (AnswerIdDataset ds : testSets) {
			ds.extractFeaturesAndLabels(featureExtractor);
		}
	}
	
	public void sanityCheck1() {
		// 1. Different datasets should have disjoint sentence Ids.
		System.out.println("======= Sanity Check1: Sentence Overlap =======");
		int overlap = 0;
		HashSet<Integer> sids = new HashSet<Integer>();
		sids.addAll(trainSet.getSentenceIds());
		for (AnswerIdDataset ds : testSets) {
			Collection<Integer> sids2 = ds.getSentenceIds();
			for (int sid : sids2) {
				if (sids.contains(sid)) {
					System.out.println("Overlap!!\t" + sid);
					overlap++;
				}
			}
			sids.addAll(sids2);
		}
		if (overlap == 0) {
			System.out.println("======= Sanity Check1 Passed: No Overlap =======");
		} else {
			System.out.println(
					String.format("Sanity Check1 Failed with %d overlapping sentences.",
					overlap));
		}
	}
	
	public double[][] trainAndPredict(LiblinearHyperParameters prm) {
		int numFeatures = featureExtractor.numFeatures();
		Model model = train(
				trainSet.features,
				trainSet.labels,
				numFeatures, prm);
		double[][] results = new double[testSets.size() + 1][];
		results[0] = predictAndEvaluate(trainSet, model, "");
		for (int i = 0; i < testSets.size(); i++) {
			AnswerIdDataset ds = testSets.get(i);
			results[i + 1] = predictAndEvaluate(ds, model,
					String.format(ds.datasetName + ".debug.txt"));
		}
		return results;
	}
	
	public void outputFeatures(Model model) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(
					new FileWriter(new File(config.featureOutputPath)));
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
		return predictAndEvaluate(ds.samples, ds.features, ds, model,
					debugFilePath);
	}

	private double[] predictAndEvaluate(
			ArrayList<QASample> samples,
			Feature[][] features,
			AnswerIdDataset ds,
			Model model,
			String debugFilePath) {
	
		int numQuestions = ds.answerFlags.length;
		HashSet<Integer> evalQIds = new HashSet<Integer>();
		double[][] predScores = new double[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			predScores[i] = new double[ds.answerFlags[i].length];
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
		
		int numMatched = 0;
		int numContained = 0;
		double avgMatchedWords = .0;
		@SuppressWarnings("unused")
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
			QAPair qa = ds.questions.get(qid);
			int length = predScores[qid].length;
			int bestIdx = -1;
			if (config.useSpanBasedSamples) {
				double matched = AnswerIdEvaluator.evaluateAccuracy(
						predScores[qid],
						ds.answerFlags[qid],
						0.5 /* threshold */);
				avgMatchedWords += matched;
				int numGold = 0;
				for (int flag : ds.answerFlags[qid]) {
					numGold += (flag > 0 ? 1 : 0);
				}
				allNegBaseline +=
						(1.0 - 1.0 * numGold / ds.answerFlags[qid].length);
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
				if (bestIdx > -1 && ds.answerFlags[qid][bestIdx] > 0) {
					numContained ++;
				}
				if (bestIdx > -1 && ds.answerHeads[qid][bestIdx] > 0) {
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
		if (config.useSpanBasedSamples) {
			allNegBaseline /= evalQIds.size();
			System.out.println("All negative baseline:\t" + evalQIds.size());
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
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
		BaselineAnswerIdExperiment exp = null;
		String answerIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new BaselineAnswerIdExperiment(answerIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ArrayList<LiblinearHyperParameters> prms =
				new ArrayList<LiblinearHyperParameters>();
		for (String prmStr : exp.config.liblinParameters) {
			prms.add(new LiblinearHyperParameters(prmStr));
		}
		double[][][] results = new double[prms.size()][][];
		for (int i = 0; i < prms.size(); i++) {
			LiblinearHyperParameters prm = prms.get(i);
			System.out.println(prm.toString());
			results[i] = exp.trainAndPredict(prm);
		}
		for (int i = 0; i < prms.size(); i++) {
			double[][] acc = results[i];
			System.out.println(prms.get(i).toString());
			System.out.println(String.format(
					"Training accuracy on %s:\t%.4f\t%.4f",
						exp.trainSet.datasetName, acc[0][0], acc[0][1]));
			for (int j = 0; j < exp.testSets.size(); j++) {
				AnswerIdDataset ds = exp.testSets.get(j);
				System.out.println(String.format(
						"Testing accuracy on %s:\t%.4f\t%.4f",
							ds.datasetName, acc[j+1][0], acc[j+1][1]));
			}
		}
		//LiblinearHyperParameters bestPar = exp.runCrossValidation(cvPrms);
		//exp.trainAndPredict(bestPar);
	}
}


/*
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

private double crossValidate(AnswerIdDataset ds, int cvFolds,
			LiblinearHyperParameters prm) {
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();		
		for (int i = 0; i < ds.getQuestions().size(); i++) {
			shuffledIds.add(i);
		}
		Collections.shuffle(shuffledIds, new Random(config.randomSeed));
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
	
*/
