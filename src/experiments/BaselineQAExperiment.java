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
import java.util.Random;

import syntax.AnswerIdFeatureExtractor;
import syntax.KBestParseRetriever;
import syntax.QASample;
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
import evaluation.F1Metric;

public class BaselineQAExperiment {

	private static String trainFilePath = "data/odesk_s1200.train.qa";
	private static String testFilePath = "data/odesk_s1200.test.qa";
	
	private static String trainSamplesPath = "odesk_s1200_20best.train.qaSamples";
	private static String testSamplesPath = "odesk_s1200_20best.test.qaSamples";
	
	private static final int randomSeed = 12345;
	
	private static void loadData(String filePath, Corpus corpus,
			ArrayList<AnnotatedSentence> annotations) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
		
		String currLine = "";
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.trim().split("\t");
			assert (info.length == 2);
			currLine = reader.readLine();
			String[] tokens = currLine.trim().split("\\s+");
			int[] tokenIds = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				tokenIds[i] = corpus.wordDict.addString(tokens[i]);
			}
			Sentence sent = new Sentence(tokenIds, corpus, corpus.sentences.size());
			sent.source = info[0];
			corpus.sentences.add(sent);
			int numProps = Integer.parseInt(info[1]);
			AnnotatedSentence annotSent = new AnnotatedSentence(sent);
			for (int i = 0; i < numProps; i++) {
				currLine = reader.readLine();
				info = currLine.split("\t");
				int propHead = Integer.parseInt(info[0]);
				int numQA = Integer.parseInt(info[2]);
				annotSent.addProposition(propHead);
				for (int j = 0; j < numQA; j++) {
					currLine = reader.readLine();
					info = currLine.split("\t");
					assert (info.length == 9);
					String[] question = new String[7];
					for (int k = 0; k < 7; k++) {
						question[k] = (info[k].equals("_") ? "" : info[k]);
					}
					QAPair qa = new QAPair(sent, propHead, question, "", null);
					String[] answers = info[8].split("###");
					for (String answer : answers) {
						qa.addAnswer(answer);
					}
					annotSent.addQAPair(propHead, qa);
				}
			}
			reader.readLine();
			annotations.add(annotSent);
		}
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.",
				annotations.size(), filePath));
	}
	
	private static void extractFeatures(ArrayList<QASample> samples,
			AnswerIdFeatureExtractor featureExtractor,
			Feature[][] features, double[] labels) {
		for (int i = 0; i < samples.size(); i++) {
			QASample sample = samples.get(i);
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			features[i] = new Feature[fv.size()];
			int[] fids = Arrays.copyOf(fv.keys(), fv.size());
			Arrays.sort(fids);
			for (int j = 0; j < fids.length; j++) {
				// Liblinear feature id starts from 1.
				features[i][j] = new FeatureNode(fids[j] + 1, fv.get(fids[j]));
			}
			labels[i] = (sample.isPositiveSample ? 1.0 : -1.0);
		}
	}
	
	private static Model train(Feature[][] features, double[] labels,
			int numFeatures, LiblinearHyperParameters par) {
		Problem training = new Problem();
		training.l = features.length;
		training.n = numFeatures;
		training.x = features;
		training.y = labels;
		Parameter parameter = new Parameter(par.solverType, par.C, par.eps);
		return Linear.train(training, parameter);
	}
	
	private static F1Metric predictAndEvaluate(Feature[][] features,
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
	
	private static double[] crossValidate(Feature[][] features, double[] labels,
			int numFeatures, int cvFolds, LiblinearHyperParameters par) {
		ArrayList<Integer> shuffledIds = new ArrayList<Integer>();
		for (int i = 0; i < features.length; i++) {
			shuffledIds.add(i);
		}
		Collections.shuffle(shuffledIds, new Random(randomSeed));
		int sampleSize = shuffledIds.size();
		int foldSize = sampleSize / cvFolds;
		Feature[][] cvTrains = new Feature[sampleSize - foldSize][];		
		Feature[][] cvValidates = new Feature[foldSize][];
		double[] cvTrainLabels = new double[sampleSize - foldSize];
		double[] cvValidateLabels = new double[foldSize];
		double avgPrec = .0, avgRecall = .0, avgF1 = .0;
		for (int i = 0; i < cvFolds; i++) {
			int numTrains = 0, numValidates = 0;
			for (int j = 0; j < sampleSize; j++) {
				if (j >= foldSize * i && j < foldSize * (i + 1)) {
					cvValidates[numValidates] = features[j];
					cvValidateLabels[numValidates++] = labels[j];
				} else {
					cvTrains[numTrains] = features[j];
					cvTrainLabels[numTrains++] = labels[j];
				}
			}
			Model model = train(cvTrains, cvTrainLabels, numFeatures, par);
			F1Metric f1 = predictAndEvaluate(cvValidates, cvValidateLabels,
					model);
			System.out.println(String.format("Using %d training and %d dev samples.",
					cvTrains.length, cvValidates.length));
			System.out.println(String.format("Cross validation fold %d:\t %s",
					i, f1.toString()));
			avgPrec += f1.precision();
			avgRecall += f1.recall();
			avgF1 += f1.f1();
		}
		return new double[] {avgPrec / cvFolds,
							 avgRecall / cvFolds,
							 avgF1 / cvFolds};
	}
	
	@SuppressWarnings("unused")
	private static void generateAndSaveQASamples(
			ArrayList<AnnotatedSentence> trains,
			ArrayList<AnnotatedSentence> tests,
			int kBest,
			ArrayList<QASample> trainSamples,
			ArrayList<QASample> testSamples) {
		Corpus corpus = trains.get(0).sentence.corpus;
		UniversalPostagMap umap = ExperimentUtils.loadPostagMap();
		
		KBestParseRetriever.generateTrainingSamples(corpus, trains, umap,
				kBest, trainSamples);
		KBestParseRetriever.generateTrainingSamples(corpus, tests, umap,
				kBest, testSamples);
		
		// Cache qaSamples to file because parsing is slow.
		ObjectOutputStream ostream = null;
		try {
			ostream = new ObjectOutputStream(new FileOutputStream(trainSamplesPath));
			ostream.writeObject(trainSamples);
			ostream.flush();
			ostream.close();
			ostream = new ObjectOutputStream(new FileOutputStream(testSamplesPath));
			ostream.writeObject(testSamples);
			ostream.flush();
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	private static void loadQASamples(
			ArrayList<QASample> trainSamples, ArrayList<QASample> testSamples) {
		ObjectInputStream istream = null;
		ArrayList<Object> objs = null;
		try {
			istream = new ObjectInputStream(new FileInputStream(trainSamplesPath));
			objs = (ArrayList<Object>) istream.readObject();
			for (int i = 0; i < objs.size(); i++) {
				trainSamples.add((QASample) objs.get(i));
			}
			istream.close();
			istream = new ObjectInputStream(new FileInputStream(testSamplesPath));
			objs = (ArrayList<Object>) istream.readObject();
			for (int i = 0; i < objs.size(); i++) {
				testSamples.add((QASample) objs.get(i));
			}
			istream.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(String.format("Read %d samples from %s",
				trainSamples.size(), trainSamplesPath));
		System.out.println(String.format("Read %d samples from %s",
				testSamples.size(), testSamplesPath));
	}
	
	public static void main(String[] args) {
		Corpus corpus = new Corpus("qa-text-corpus");
		ArrayList<AnnotatedSentence> trains = new ArrayList<AnnotatedSentence>(),
									 tests = new ArrayList<AnnotatedSentence>();
		ArrayList<QASample> trainSamples = new ArrayList<QASample>(),
							testSamples = new ArrayList<QASample>(),
							allSamples = new ArrayList<QASample>();
		
		Feature[][] trainFeats, testFeats;
		double[] trainLabels, testLabels;
		int cvFolds = 5, minFeatureFreq = 5, kBest = 20;

		try { 
			loadData(trainFilePath, corpus, trains);
			loadData(testFilePath, corpus, tests);
		} catch (IOException e) {
			e.printStackTrace();	
		}
		
		generateAndSaveQASamples(trains, tests, kBest, trainSamples, testSamples);
		if (trainSamples.size() == 0) {
			loadQASamples(trainSamples, testSamples);
		}
		
		System.out.println(String.format(
				"Start processing %d training and %d test samples.",
					trainSamples.size(), testSamples.size()));

		AnswerIdFeatureExtractor featureExtractor =
				new AnswerIdFeatureExtractor(corpus, kBest, minFeatureFreq);
		allSamples.addAll(trainSamples);
		allSamples.addAll(testSamples);
		featureExtractor.extractFeatures(allSamples);
		int numFeatures = featureExtractor.numFeatures();
		
		trainFeats = new Feature[trainSamples.size()][];
		testFeats = new Feature[testSamples.size()][];
		trainLabels = new double[trainSamples.size()];
		testLabels = new double[testSamples.size()];
		
		extractFeatures(trainSamples, featureExtractor, trainFeats, trainLabels);
		extractFeatures(testSamples, featureExtractor, testFeats, testLabels);
		
		ArrayList<LiblinearHyperParameters> cvPars =
				new ArrayList<LiblinearHyperParameters>();
		LiblinearHyperParameters bestPar = null;
		ArrayList<double[]> cvResults = new ArrayList<double[]>();
		double bestF1 = .0;
		
		cvPars.add(new LiblinearHyperParameters(SolverType.L2R_LR, 1.0, 1e-2));
		cvPars.add(new LiblinearHyperParameters(SolverType.L2R_LR, 10.0, 1e-2));
		cvPars.add(new LiblinearHyperParameters(SolverType.L2R_LR, 0.1, 1e-2));
		//cvPars.add(new LiblinearHyperParameters(SolverType.L1R_LR, 0.1, 1e-2));
		//cvPars.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVC, 0.1, 1e-3));
		//cvPars.add(new LiblinearHyperParameters(SolverType.L2R_L2LOSS_SVR, 0.1, 1e-3));
		
		for (LiblinearHyperParameters par : cvPars) {
			double[] res = crossValidate(trainFeats, trainLabels, numFeatures,
					cvFolds, par);
			cvResults.add(res);
		}
		
		for (int i = 0; i < cvResults.size(); i++) {
			double[] res = cvResults.get(i);
			System.out.println(String.format("%s\t%.3f\t%.3f\t%.3f\n",
					cvPars.get(i).toString(), res[0], res[1], res[2]));
			if (res[2] > bestF1) {
				bestF1 = res[2];
				bestPar = cvPars.get(i);
			}
		}
		
		Model model = train(trainFeats, trainLabels, numFeatures, bestPar);
		F1Metric f1 = predictAndEvaluate(trainFeats, trainLabels, model);
		System.out.println("Training accuracy:\t" + f1.toString());
		f1 = predictAndEvaluate(testFeats, testLabels, model);
		System.out.println("Testing accuracy:\t" + f1.toString());
		
		
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
}
