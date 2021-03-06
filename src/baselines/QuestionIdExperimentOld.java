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
import evaluation.F1Metric;
import experiments.LiblinearHyperParameters;

/***** backup: with label aggregation ******/
public class QuestionIdExperimentOld {
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
	
	public QuestionIdExperimentOld(String questionIdConfigPath)
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
				HashMap<String, String> slots = new HashMap<String, String>();
				for (QAPair qa : sent.qaLists.get(propHead)) {
					String[] temp = QuestionEncoder.getLabels(qa.questionWords);
					for (String lb : temp) {
						if (!lb.contains("=")) {
							continue;
						}
						String pfx = lb.split("=")[0];
						String val = lb.split("=")[1];
						if (slots.containsKey(pfx) &&
							!slots.get(pfx).equals(val)) {
							slots.put(pfx, "something");
						} else {
							slots.put(pfx, val);
						}
						if (!config.aggregateLabels) {
							labelDict.addString(lb);
						}
					}
					tempDict.addString(getTemplateString(temp));
				}
				if (config.aggregateLabels) {
					for (String pfx : slots.keySet()) {
						labelDict.addString(pfx + "=" + slots.get(pfx));
					}
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
				config.useDependencyFeatures,
				config.use1BestFeatures);
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
				if (qgenWriter != null) {
				/*	ArrayList<String[]> questions = qgen.generateQuestions(
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
					}*/
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

}
