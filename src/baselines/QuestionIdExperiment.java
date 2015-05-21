package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
import evaluation.F1Metric;
import experiments.LiblinearHyperParameters;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QuestionIdExperiment {
	private QuestionIdConfig config;
	
	private Corpus baseCorpus; 
	private QuestionIdFeatureExtractor featureExtractor;
	private QuestionGenerator qgen;
	CountDictionary labelDict, tempDict;
	
	private QuestionIdDataset trainSet;
	private ArrayList<QuestionIdDataset> testSets;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	public QuestionIdExperiment(String questionIdConfigPath)
			throws IOException {
		config = questionIdConfigPath.isEmpty() ? new QuestionIdConfig():
					new QuestionIdConfig(questionIdConfigPath);
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new ArrayList<QuestionIdDataset>();
		
		// ********** Config and load QA Data ********************
		trainSet = new QuestionIdDataset(baseCorpus,
				StringUtils.join("_", config.trainSets));
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
		//tempDict.prettyPrint();
		
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
		
		// ********** Extract features ***************
		featureExtractor = new QuestionIdFeatureExtractor(
				baseCorpus,
				config.kBest,
				config.minFeatureFreq,
				config.useLexicalFeatures,
				config.useDependencyFeatures);
		featureExtractor.extractFeatures(trainSet.samples);
		trainSet.extractFeaturesAndLabels(featureExtractor);
		for (QuestionIdDataset ds : testSets) {
			ds.extractFeaturesAndLabels(featureExtractor);
		}
	}
	
	private String getTemplateString(String[] temp) {
		String[] shortTemp = new String[temp.length];
		for (int i = 0; i < temp.length; i++) {
			String s = temp[i].split("=")[0];
			shortTemp[i] = s.contains("_") ? s.split("_")[0] + "_PP" : s;
		}
		return StringUtils.join("\t", "_", shortTemp);
	}
	
	public double[][][] trainAndPredict(LiblinearHyperParameters prm,
										int topK,
										boolean getPrecRecallCurve,
										boolean generateQuestions) {
		System.out.println(String.format("Training with %d samples.",
				trainSet.features.length));
		int numFeatures = featureExtractor.numFeatures();
		Model model = train(
				trainSet.features,
				trainSet.labels,
				numFeatures, prm);
		
		// TODO: FIXME
		if (generateQuestions) {
			System.out.println("Preparing to generate questions.");
			qgen = new QuestionGenerator(baseCorpus, labelDict, tempDict);		
			for (QuestionIdDataset ds : testSets) {
				if (ds.datasetName.contains("dev")) {
					generateQuestions(ds, model, config.evalThreshold);
				}
			}
		}
		double[][][] results = new double[testSets.size() + 1][][];
		results[0] = new double[1][];
		results[0][0] = predictAndEvaluate(trainSet, model, topK, "");
		System.out.println(String.format("Training accuracy on %s:\t%s",
				trainSet.datasetName,
				StringUtils.doubleArrayToString("\t", results[0][0])));
		
		for (int d = 0; d < testSets.size(); d++) {
			QuestionIdDataset ds = testSets.get(d);
			if (getPrecRecallCurve) {
				results[d+1] = new double[labelDict.size()][];
				for (int k = 1; k <= labelDict.size(); k++) {
					results[d+1][k-1] = predictAndEvaluate(ds, model, k, "");
				}
			} else {
				results[d+1] = new double[1][];
				results[d+1][0] = predictAndEvaluate(ds, model, topK, "");
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
			int topK,
			String debugFilePath) {
		// Aggregate results
		HashMap<Integer, HashMap<Integer, HashMap<String, String>>> slots =
			new HashMap<Integer, HashMap<Integer, HashMap<String, String>>>();
		HashMap<Integer, HashMap<Integer, HashMap<String, Double>>> scores =
				new HashMap<Integer, HashMap<Integer, HashMap<String, Double>>>();
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			slots.put(sid, new HashMap<Integer, HashMap<String, String>>());
			scores.put(sid, new HashMap<Integer, HashMap<String, Double>>());
			for (int pid : sent.qaLists.keySet()) {
				slots.get(sid).put(pid, new  HashMap<String, String>());
				scores.get(sid).put(pid, new  HashMap<String, Double>());
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
		}
		// Get top K
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			for (int pid : sent.qaLists.keySet()) {
				ArrayList<Double> scRank = new ArrayList<Double>();
				for (double s : scores.get(sid).get(pid).values()) {
					scRank.add(s);
				}
				Collections.sort(scRank, Collections.reverseOrder());
				double threshold = scRank.get(Math.min(scRank.size()-1, topK));
				// System.out.println(topK + ", " + threshold);
				HashMap<String, String> sl = slots.get(sid).get(pid);
				HashMap<String, Double> sc = scores.get(sid).get(pid);
				for (String k : sc.keySet()) {
					if (sc.get(k) < threshold) {
						sl.remove(k);
					}
				}
			}
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
	
	private void generateQuestions(QuestionIdDataset ds, Model model,
			double threshold) {
		HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results =
			new HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>>();
		
		for (int i = 0; i < ds.samples.size(); i++) {
			QASample sample = ds.samples.get(i);
			double[] prob = new double[2];
			Linear.predictProbability(model, ds.features[i], prob);
			if (prob[0] < threshold) {
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
				ArrayList<String[]> questions = qgen.generateQuestions(
						sent, propHead, results.get(sentId).get(propHead));
				if (questions == null) {
					continue;
				}
				System.out.println(sent.getTokensString());
				System.out.println(sent.getTokenString(propHead));
				System.out.println("=========== annotated ==============");
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					System.out.println(qa.getQuestionString() + "\t" +
							qa.getAnswerString());
				}
				System.out.println("=========== generated ==============");
				for (String[] question : questions) {
					System.out.println(StringUtils.join("\t", question));
				}
				System.out.println();
			}
		}
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
		for (String prmStr : exp.config.liblinParameters) {
			prms.add(new LiblinearHyperParameters(prmStr));
		}
		double[][][][] results = new double[prms.size()][][][];
		for (int i = 0; i < prms.size(); i++) {
			LiblinearHyperParameters prm = prms.get(i);
			System.out.println(prm.toString());
			results[i] = exp.trainAndPredict(prm,
				exp.config.evalTopK,
				true,  /* get precision-reall curve */
				false  /* generate question */);
		}
		System.out.println("====== training finished =======");
		for (int i = 0; i < prms.size(); i++) {
			double[][][] res = results[i];
			System.out.println(prms.get(i).toString());
			System.out.println(String.format(
					"Training accuracy on %s:\t%s",
						exp.trainSet.datasetName,
						StringUtils.doubleArrayToString("\t", res[0][0])));
			for (int j = 0; j < exp.testSets.size(); j++) {
				QuestionIdDataset ds = exp.testSets.get(j);
				System.out.println(String.format(
						"Testing accuracy on %s", ds.datasetName));
				for (int k = 0; k < res[j+1].length; k++) {
					System.out.println(k + "\t" +
						StringUtils.doubleArrayToString("\t", res[j+1][k]));
				}
			}
		}
		// TODO Pick best PRM
		exp.trainAndPredict(prms.get(0),
				exp.config.evalTopK,
				false,  /* get precision-reall curve */
				true /* generate question */);
	}
}
