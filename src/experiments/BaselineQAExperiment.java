package experiments;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import syntax.KBestFeatureExtractor;
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

	private static String trainFilePath = "data/odesk_s700.train.qa";
	private static String testFilePath = "data/odesk_s700.test.qa";
	
	private static String trainSamplesPath = "odesk_s700.train.qaSamples";
	private static String testSamplesPath = "odesk_s700.test.qaSamples";
	
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
			KBestFeatureExtractor featureExtractor,
			Feature[][] features, double[] labels) {
		for (int i = 0; i < samples.size(); i++) {
			QASample sample = samples.get(i);
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			features[i] = new Feature[fv.size()];
			int[] fids = Arrays.copyOf(fv.keys(), fv.size());
			Arrays.sort(fids);
			for (int j = 0; j < fids.length; j++) {
				features[i][j] = new FeatureNode(fids[j] + 1, fv.get(fids[j]));
			}
			labels[i] = (sample.isPositiveSample ? 1.0 : -1.0);
		}
	}
	
	private static Model train(Feature[][] features, double[] labels,
			int numFeatures) {
		Problem training = new Problem();
		training.l = features.length;
		training.n = numFeatures;
		training.x = features;
		training.y = labels;
		
		// L2-regularized logistic regression (primal) - l2r_lr
		SolverType solver = SolverType.L2R_LR;
		double C = 1.0,  eps = 0.01;
		Parameter parameter = new Parameter(solver, C, eps);
		return Linear.train(training, parameter);
	}
	
	private static void predictAndEvaluate(Feature[][] features, double[] labels,
			Model model) {
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
		F1Metric f1 = new F1Metric(numMatched, numGold, numPred);
		System.out.println(f1.toString());
	}
	
	public static void main(String[] args) {
		Corpus corpus = new Corpus("qa-text-corpus");
		UniversalPostagMap umap = ExperimentUtils.loadPostagMap();
		ArrayList<AnnotatedSentence> trains = new ArrayList<AnnotatedSentence>(),
								     tests = new ArrayList<AnnotatedSentence>();
		ArrayList<QASample> trainSamples = new ArrayList<QASample>(),
						    testSamples = new ArrayList<QASample>(),
						    allSamples = new ArrayList<QASample>();
		
		Feature[][] trainFeats, testFeats;
		double[] trainLabels, testLabels;
		
		try { 
			loadData(trainFilePath, corpus, trains);
			loadData(testFilePath, corpus, tests);
		} catch (IOException e) {
			e.printStackTrace();	
		}
		
		int kBest = 5;
		KBestParseRetriever.generateTrainingSamples(corpus, trains, umap,
				kBest, trainSamples);
		KBestParseRetriever.generateTrainingSamples(corpus, tests, umap,
				kBest, testSamples);
		
		// Cache qaSamples to file because parsing is slow
		/*
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(trainSamplesPath));
			out.writeObject(trainSamples);
			out.flush();
			out.close();
			out = new ObjectOutputStream(new FileOutputStream(testSamplesPath));
			out.writeObject(testSamples);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		KBestFeatureExtractor featureExtractor =
				new KBestFeatureExtractor(corpus, 3);
		allSamples.addAll(trainSamples);
		allSamples.addAll(testSamples);
		featureExtractor.extractFeatures(allSamples);
		
		trainFeats = new Feature[trainSamples.size()][];
		testFeats = new Feature[testSamples.size()][];
		trainLabels = new double[trainSamples.size()];
		testLabels = new double[testSamples.size()];
		
		extractFeatures(trainSamples, featureExtractor, trainFeats, trainLabels);
		extractFeatures(testSamples, featureExtractor, testFeats, testLabels);
		
		Model model = train(trainFeats, trainLabels, featureExtractor.numFeatures());
		System.out.println("Training accuracy:\t");
		predictAndEvaluate(testFeats, testLabels, model);
		System.out.println("Testing accuracy:\t");
		predictAndEvaluate(testFeats, testLabels, model);
		
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("feature_weights.csv"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for (int fid = 0; fid < model.getNrFeature(); fid++) {
			double fweight = model.getFeatureWeights()[fid + 1];
			System.out.println(String.format("%s,%.6f",
					featureExtractor.featureDict.getString(fid),
					fweight));
		}

	}
}
