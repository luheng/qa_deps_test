package baselines;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import util.StrUtils;
import learning.QASample;
import learning.QuestionIdDataset;
import data.AnnotatedSentence;
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

public class QuestionIdExperiment2 extends QuestionIdExperiment {
	
/*	private static String[] classes = new String[] {
		"W0", "W1", "W2", "WHERE", "WHEN", "HOW", "HOW MUCH", "WHY"
	};
*/	
	CountDictionary classes;
	HashMap<String, Model> models;
	HashMap<String, Feature[][]> trainFeatures;
	HashMap<String, double[]> trainLabels;
	
	public QuestionIdExperiment2(String questionIdConfigPath)
			throws IOException {
		super(questionIdConfigPath);
		models = new HashMap<String, Model>();
		trainFeatures = new HashMap<String, Feature[][]>();
		trainLabels = new HashMap<String, double[]>();
		
		classes = new CountDictionary();
		for (QASample sample : trainSet.samples) {
			String cl  = sample.questionLabel.split("=")[0].split("_")[0];
			classes.addString(cl);
		}
		classes.prettyPrint();
		for (String cl : classes.getStrings()) {
			int numSamples = classes.getCount(cl);
			Feature[][] feats = new Feature[numSamples][];
			double[] labels = new double[numSamples];
			int cnt = 0;
			for (int i = 0; i < trainSet.samples.size(); i++) {
				QASample sample = trainSet.samples.get(i);
				if (cl.equals(sample.questionLabel.split("=")[0].split("_")[0])) {
					feats[cnt] = trainSet.features[i];
					labels[cnt++] = trainSet.labels[i];
				}
			}
			trainFeatures.put(cl, feats);
			trainLabels.put(cl, labels);
		}
	}
	
	public double[][][] trainAndPredict(LiblinearHyperParameters prm,
										double threshold,
										int topK,
										boolean getPrecRecallCurve,
										String qgenPath,
										String debugPath) {
		System.out.println(String.format("Training with %d samples.",
				trainSet.features.length));
		int numFeatures = featureExtractor.numFeatures();
		
		for (String cl : classes.getStrings()) {
			Model model = train(cl, trainFeatures.get(cl), trainLabels.get(cl),
					numFeatures, prm);
			models.put(cl, model);
		}
		
		double[][][] results = new double[testSets.size() + 1][][];
		results[0] = new double[1][];
		results[0][0] = predictAndEvaluate(
				trainSet, models, threshold, topK, "", "");
		System.out.println(String.format("Training accuracy on %s:\t%s",
				trainSet.datasetName,
				StrUtils.doubleArrayToString("\t", results[0][0])));
		
		for (int d = 0; d < testSets.size(); d++) {
			QuestionIdDataset ds = testSets.get(d);
			if (getPrecRecallCurve) {
				if (threshold > 0) {
					results[d+1] = new double[config.numPRCurvePoints][];
					for (int k = 0; k < config.numPRCurvePoints; k++) {
						double thr = 1.0 * k / config.numPRCurvePoints;
						results[d+1][k] = predictAndEvaluate(
								ds, models, thr, -1, "", "");
					}
				} else {
					results[d+1] = new double[labelDict.size()][];
					for (int k = 1; k <= labelDict.size(); k++) {
						results[d+1][k-1] = predictAndEvaluate(
								ds, models, -1.0, k, "", "");
					}
				}
			} else {
				results[d+1] = new double[1][];
				results[d+1][0] = predictAndEvaluate(
						ds, models, threshold, topK,
						qgenPath.isEmpty() ? "" : qgenPath + ds.datasetName,
						debugPath.isEmpty() ? "" : debugPath + ds.datasetName);
			}
		}
		return results;
	}
	
	private Model train(String cl, Feature[][] features, double[] labels,
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
			QuestionIdDataset ds,
			HashMap<String, Model> models,
			double evalThreshold,
			int evalTopK,
			String qgenPath,
			String debugPath) {
		// Aggregate results
		HashMap<Integer, HashMap<Integer, HashMap<String, String>>> slots =
			new HashMap<Integer, HashMap<Integer, HashMap<String, String>>>();
		HashMap<Integer, HashMap<Integer, HashMap<String, Double>>> scores =
				new HashMap<Integer, HashMap<Integer, HashMap<String, Double>>>();
		HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> goldLabels =
				new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			slots.put(sid, new HashMap<Integer, HashMap<String, String>>());
			scores.put(sid, new HashMap<Integer, HashMap<String, Double>>());
			goldLabels.put(sid, new HashMap<Integer, ArrayList<Integer>>());
			for (int pid : sent.qaLists.keySet()) {
				slots.get(sid).put(pid, new  HashMap<String, String>());
				scores.get(sid).put(pid, new  HashMap<String, Double>());
				goldLabels.get(sid).put(pid, new  ArrayList<Integer>());
			}
		}

		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int sid = sample.sentenceId;
			int pid = sample.propHead;
			String cl = sample.questionLabel.split("=")[0].split("_")[0];
			Model model = models.get(cl);
			double[] prob = new double[2];
			Linear.predictProbability(model, ds.features[i], prob);
			HashMap<String, String> sl = slots.get(sid).get(pid);
			HashMap<String, Double> sc = scores.get(sid).get(pid);
			String lb = sample.questionLabel;
			if (config.aggregateLabels) {
				String pfx = lb.split("=")[0];
				String val = lb.split("=")[1];
				if (!sc.containsKey(pfx) || sc.get(pfx) < prob[0]) {
					sl.put(pfx, val);
					sc.put(pfx, prob[0]);
				}
			} else {
				sl.put(lb, "");
				sc.put(lb, prob[0]);
			}
			if (sample.isPositiveSample) {
				goldLabels.get(sid).get(pid).add(sample.questionLabelId);
			}
		}
		
		BufferedWriter qgenWriter = null, debugWriter = null;
		try {
			if (!qgenPath.isEmpty()) {
				qgenWriter = new BufferedWriter(new FileWriter(new File(qgenPath)));			
			}
			if (!debugPath.isEmpty()) {
				debugWriter = new BufferedWriter(new FileWriter(new File(debugPath)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			for (int pid : sent.qaLists.keySet()) {
				HashMap<String, String> sl = slots.get(sid).get(pid);
				HashMap<String, Double> sc = scores.get(sid).get(pid);
				HashMap<String, Double> labels = new HashMap<String, Double>();
				double thr = evalThreshold;
				if (thr < 0) {
					// Get top K
					ArrayList<Double> scRank = new ArrayList<Double>();
					for (double s : scores.get(sid).get(pid).values()) {
						scRank.add(s);
					}
					Collections.sort(scRank, Collections.reverseOrder());
					thr = scRank.get(Math.min(scRank.size(), evalTopK) - 1);
				}
				for (String k : sc.keySet()) {
					if (sc.get(k) < thr) {
						sl.remove(k);
					}
				}
				for (String k : sl.keySet()) {
					String lb = config.aggregateLabels ? k + "=" + sl.get(k) : k;
					labels.put(lb, sc.get(k));
				}
				//System.out.println(topK + ", " + thr + ", " + labels.size());
				// Generate questions
				if (debugWriter != null) {
					try {
						debugWriter.write("\n" + sent.sentence.getTokensString());
						debugWriter.write("\n" + sent.sentence.getTokenString(pid));
						debugWriter.write("\nGold:\t");
						for (int id : goldLabels.get(sid).get(pid)) {
							debugWriter.write(labelDict.getString(id) + "\t");
						}
						debugWriter.write("\nPred:\t");
						for (String lb : labels.keySet()) {
							debugWriter.write(String.format("%s,%.2f\t", lb, labels.get(lb)));
						}
						debugWriter.write("\nRecall loss:\t");
						for (int id : goldLabels.get(sid).get(pid)) {
							String lb = labelDict.getString(id);
							if (!labels.containsKey(lb)) {
								debugWriter.write(String.format(
										"%s,%.2f\t", lb, sc.get(lb)));
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
/*				if (qgenWriter != null) {
					ArrayList<String[]> questions = qgen.generateQuestions(
							sent.sentence, pid, labels);
					if (questions == null) {
						continue;
					}					
					try {
						qgenWriter.write(sent.sentence.getTokensString() + "\n");
						qgenWriter.write(sent.sentence.getTokenString(pid) + "\n");
						qgenWriter.write("=========== annotated ==============\n");
						for (QAPair qa : sent.qaLists.get(pid)) {
							qgenWriter.write(qa.getQuestionString() + "\t" +
									qa.getAnswerString() + "\n");
						}
						qgenWriter.write("=========== generated ==============\n");
						for (String[] question : questions) {
							qgenWriter.write(StrUtils.join("\t", question) + "\n");
						}
						qgenWriter.write("\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}*/
			}
		}
		try {
			if (qgenWriter != null) {
				qgenWriter.close();
			}
			if (debugWriter != null) {
				debugWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		F1Metric f1 = new F1Metric();
		int numCorrect = 0;
		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int sid = sample.sentenceId;
			int pid = sample.propHead;
			String lb = sample.questionLabel;
			HashMap<String, String> sl = slots.get(sid).get(pid);
			int gold = sample.isPositiveSample ? 1 : -1;
			int pred = 0;
			if (config.aggregateLabels) {
				String pfx = lb.split("=")[0];
				String val = lb.split("=")[1];
				pred = (sl.containsKey(pfx) && sl.get(pfx).equals(val)) ? 1 : -1;
			} else {
				pred = sl.containsKey(lb) ? 1 : -1;
			}
			f1.numGold += (gold > 0 ? 1 : 0);
			f1.numProposed += (pred > 0 ? 1 : 0);
			f1.numMatched += ((gold > 0 && pred > 0) ? 1 : 0);
			numCorrect += (gold == pred ? 1 : 0);
		}
		return new double[] {1.0 * numCorrect / ds.samples.size(),
				f1.precision(), f1.recall(), f1.f1()}; 	
	}
	
	public static void main(String[] args) {
		QuestionIdExperiment2 exp = null;
		String questionIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new QuestionIdExperiment2(questionIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ArrayList<LiblinearHyperParameters> prms =
				new ArrayList<LiblinearHyperParameters>();
		/*
		for (String prmStr : exp.config.liblinParameters) {
			prms.add(new LiblinearHyperParameters(prmStr));
		}
		*/
		/*********** Grid Search Parameters **********/
		for (double C : new double[] {0.0625, 0.125, 0.25, 0.5, 1, 2, 4, 8, 16}) {
		//for (double C : new double[] {4, 8, 16, 32, 64}) {
			for (double eps : new double[] {1e-3}) {
				prms.add(new LiblinearHyperParameters(SolverType.L1R_LR, C, eps));
				prms.add(new LiblinearHyperParameters(SolverType.L2R_LR, C, eps));
			}
		}
		
		double[][][][] results = new double[prms.size()][][][];
		for (int i = 0; i < prms.size(); i++) {
			LiblinearHyperParameters prm = prms.get(i);
			System.out.println(prm.toString());
			results[i] = exp.trainAndPredict(prm,
				exp.config.evalThreshold,
				exp.config.evalTopK,
				true,  /* get precision-reall curve */
				"",  /* qgen path */
				""   /* debug path */);
		}
	
		System.out.println("====== training finished =======");
		int bestPrmId = 0, bestK = 0;
		double bestPrmF1 = 0.0;
		for (int i = 0; i < prms.size(); i++) {
			double[][][] res = results[i];
			System.out.println(prms.get(i).toString());
			System.out.println(String.format(
					"Training accuracy on %s:\t%s",
						exp.trainSet.datasetName,
						StrUtils.doubleArrayToString("\t", res[0][0])));
			double bestPoint = 0;
			for (int j = 0; j < exp.testSets.size(); j++) {
				QuestionIdDataset ds = exp.testSets.get(j);
				System.out.println(String.format(
						"Testing accuracy on %s", ds.datasetName));
				for (int k = 0; k < res[j+1].length; k++) {
					System.out.println(k + "\t" +
						StrUtils.doubleArrayToString("\t", res[j+1][k]));
					if (res[j+1][k][3] > bestPoint) {
						bestPoint = res[j+1][k][3];
						bestK = k;
					}
				}
			}
			if (bestPoint > bestPrmF1) {
				bestPrmId = i;
				bestPrmF1 = bestPoint;
			}
		}
		System.out.println("Best PRM:\t" + prms.get(bestPrmId));
		System.out.println("Best K:\t" + bestK);
		// TODO Pick best PRM
		boolean useTopK = (exp.config.evalThreshold > 0);
		double[][][] res = exp.trainAndPredict(prms.get(bestPrmId),
				useTopK ? (1.0 * bestK / exp.config.numPRCurvePoints) : -1.0,
				useTopK ? -1 : bestK + 1,
				false,  // get precision-reall curve
				"qgen-",
				"debug-");
		for (int j = 0; j < exp.testSets.size(); j++) {
			QuestionIdDataset ds = exp.testSets.get(j);
			System.out.println(String.format(
					"Testing accuracy on %s", ds.datasetName));
			System.out.println(StrUtils.doubleArrayToString("\t", res[j+1][0]));
		}
	}
}
