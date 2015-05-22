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
import java.util.HashSet;

import config.DataConfig;
import config.QuestionIdConfig;
import util.StrUtils;
import annotation.QuestionEncoder;
import learning.KBestParseRetriever;
import learning.QASample;
import learning.QuestionIdDataset;
import learning.QuestionIdFeatureExtractor;
import data.AnnotatedSentence;
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

public class QuestionIdExperiment {
	protected QuestionIdConfig config;
	
	protected Corpus baseCorpus; 
	protected QuestionIdFeatureExtractor featureExtractor;
	protected QuestionGenerator qgen;
	CountDictionary labelDict, tempDict;
	
	protected QuestionIdDataset trainSet;
	protected ArrayList<QuestionIdDataset> testSets;
	
	protected String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	public QuestionIdExperiment(String questionIdConfigPath)
			throws IOException {
		config = questionIdConfigPath.isEmpty() ? new QuestionIdConfig():
					new QuestionIdConfig(questionIdConfigPath);
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new ArrayList<QuestionIdDataset>();
		
		System.out.println(config.toString());
		
		// ********** Config and load QA Data ********************
		trainSet = new QuestionIdDataset(baseCorpus,
				StrUtils.join("_", config.trainSets));
		for (String dsName : config.trainSets) {
			trainSet.loadData(DataConfig.getDataset(dsName));
		}
		for (String dsName : config.testSets) {
			QuestionIdDataset ds = new QuestionIdDataset(baseCorpus, dsName);
			ds.loadData(DataConfig.getDataset(dsName));
			testSets.add(ds);
		}
		
		// ************ Extract slot labels and templates **************
		labelDict = new CountDictionary();
		tempDict = new CountDictionary();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					String[] temp = QuestionEncoder.getLabels(qa.questionWords);
					for (String lb : temp) {
						if (!lb.contains("=")) {
							continue;
						}
						labelDict.addString(lb);
					}
					tempDict.addString(getTemplateString(temp));
				}
			}
		}
		assert (config.minQuestionLabelFreq == 1);
		labelDict = new CountDictionary(labelDict, config.minQuestionLabelFreq);
		labelDict.prettyPrint();
		tempDict.prettyPrint();
		
		// *********** Generate training/test samples **********
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper =
					new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, labelDict,
									 config.aggregateLabels);
			for (QuestionIdDataset ds : testSets) {
				ds.generateSamples(syntaxHelper, labelDict,
								   config.aggregateLabels);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.samples);
				ostream.flush();
				ostream.close();
				for (QuestionIdDataset ds : testSets) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(getSampleFileName(ds)));
					ostream.writeObject(ds.samples);
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
     			trainSet.loadSamples(getSampleFileName(trainSet));
     			for (QuestionIdDataset ds : testSets) {
					ds.loadSamples(getSampleFileName(ds));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		
		qgen = new QuestionGenerator(baseCorpus, labelDict, tempDict);
		
		// ********** Extract features ***************
		featureExtractor = new QuestionIdFeatureExtractor(
				baseCorpus,
				config.kBest,
				config.minFeatureFreq,
				config.useLexicalFeatures,
				config.useDependencyFeatures);
		featureExtractor.extractFeatures(trainSet.samples);
		trainSet.extractFeaturesAndLabels(featureExtractor,
				config.normalizeFeatures);
		for (QuestionIdDataset ds : testSets) {
			ds.extractFeaturesAndLabels(featureExtractor,
					config.normalizeFeatures);
		}
	}
	
	private String getTemplateString(String[] temp) {
		String[] shortTemp = new String[temp.length];
		for (int i = 0; i < temp.length; i++) {
			String s = temp[i].split("=")[0];
			shortTemp[i] = s.contains("_") ? s.split("_")[0] + "_PP" : s;
		}
		return StrUtils.join("\t", "_", shortTemp);
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
		Model model = train(
				trainSet.features,
				trainSet.labels,
				numFeatures, prm);
		
		double[][][] results = new double[testSets.size() + 1][][];
		results[0] = new double[1][];
		results[0][0] = predictAndEvaluate(
				trainSet, model, threshold, topK, "", "");
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
								ds, model, thr, -1, "", "");
					}
				} else {
					results[d+1] = new double[labelDict.size()][];
					for (int k = 1; k <= labelDict.size(); k++) {
						results[d+1][k-1] = predictAndEvaluate(
								ds, model, -1.0, k, "", "");
					}
				}
			} else {
				results[d+1] = new double[1][];
				results[d+1][0] = predictAndEvaluate(
						ds, model, threshold, topK,
						qgenPath.isEmpty() ? "" : qgenPath + ds.datasetName,
						debugPath.isEmpty() ? "" : debugPath + ds.datasetName);
			}
		}
		return results;
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
			QuestionIdDataset ds,
			Model model,
			double evalThreshold,
			int evalTopK,
			String qgenPath,
			String debugPath) {
		HashMap<Integer, HashMap<Integer, HashSet<String>>> slots =
			new HashMap<Integer, HashMap<Integer, HashSet<String>>>();
		HashMap<Integer, HashMap<Integer, HashMap<String, Double>>> scores =
				new HashMap<Integer, HashMap<Integer, HashMap<String, Double>>>();
		
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			slots.put(sid, new HashMap<Integer,  HashSet<String>>());
			scores.put(sid, new HashMap<Integer, HashMap<String, Double>>());
			for (int pid : sent.qaLists.keySet()) {
				slots.get(sid).put(pid, new HashSet<String>());
				scores.get(sid).put(pid, new  HashMap<String, Double>());
			}
		}

		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			int sid = sample.sentenceId;
			int pid = sample.propHead;
			double[] prob = new double[2];
			Linear.predictProbability(model, ds.features[i], prob);
			String lb = sample.questionLabel;
			slots.get(sid).get(pid).add(lb);
			scores.get(sid).get(pid).put(lb, prob[0]);
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
		F1Metric microF1 = new F1Metric();
		double macroPrec = .0, macroRecall = .0;
		int cnt = 0;
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			for (int pid : sent.qaLists.keySet()) {
				HashSet<String> gl = ds.goldLabels.get(sid).get(pid);
  			    HashSet<String> sl = slots.get(sid).get(pid);
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
				for (String lb : sl) {
					labels.put(lb, sc.get(lb));
				}
				F1Metric f1 = new F1Metric();
				f1.numGold = gl.size();
				f1.numProposed = labels.size();
				for (String lb : sl) {
					f1.numMatched += gl.contains(lb) ? 1 : 0;
				}
				microF1.add(f1);
				macroPrec += f1.precision();
				macroRecall += f1.recall();
				cnt ++;
				
				//System.out.println(topK + ", " + thr + ", " + labels.size());
				/************ Print debugging info ***************/
				// Generate questions
				if (debugWriter != null) {
					try {
						debugWriter.write("\n" + sent.sentence.getTokensString());
						debugWriter.write("\n" + sent.sentence.getTokenString(pid));
						debugWriter.write("\nGold:\t");
						for (String glb : gl) {
							debugWriter.write(glb + "\t");
						}
						debugWriter.write("\nPred:\t");
						for (String lb : labels.keySet()) {
							debugWriter.write(String.format("%s,%.2f\t", lb, labels.get(lb)));
						}
						debugWriter.write("\nRecall loss:\t");
						for (String glb : gl) {
							if (labels.containsKey(glb)) {
								continue;
							}
							double s = sc.containsKey(glb) ? sc.get(glb) : -1.0;
							debugWriter.write(String.format("%s,%.2f\t", glb, s));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (qgenWriter != null) {
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
				}
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
		/************* Return results *********/
		macroPrec /= cnt;
		macroRecall /= cnt;
		double macroF1 = 2 * macroPrec * macroRecall / (macroPrec + macroRecall);
		return new double[] {
				macroPrec, macroRecall, macroF1,
				microF1.precision(), microF1.recall(), microF1.f1()}; 	
	}
	
	public static void main(String[] args) {
		QuestionIdExperiment exp = null;
		String questionIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new QuestionIdExperiment(questionIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ArrayList<LiblinearHyperParameters> prms =
				new ArrayList<LiblinearHyperParameters>();
	
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
					// By macro F1
					if (res[j+1][k][2] > bestPoint) {
						bestPoint = res[j+1][k][2];
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
