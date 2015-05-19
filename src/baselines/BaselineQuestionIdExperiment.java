package baselines;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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

public class BaselineQuestionIdExperiment {
	private QuestionIdConfig config;
	
	private Corpus baseCorpus; 
	private QuestionIdFeatureExtractor featureExtractor;
	private QuestionGenerator qgen;
	
	CountDictionary slotDict, tempDict;
	
	private QuestionIdDataset trainSet;
	private ArrayList<QuestionIdDataset> testSets;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	public BaselineQuestionIdExperiment(String questionIdConfigPath)
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
			KBestParseRetriever syntaxHelper =
					new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper, slotDict);
			for (QuestionIdDataset ds : testSets) {
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
	
	public double[][] trainAndPredict(LiblinearHyperParameters prm,
			boolean generateQuestions) {
		System.out.println(String.format("Training with %d samples.",
				trainSet.features.length));
		int numFeatures = featureExtractor.numFeatures();
		Model model = train(
				trainSet.features,
				trainSet.labels,
				numFeatures, prm);
		
		if (generateQuestions) {
			System.out.println("Preparing to generate questions.");
			qgen = new QuestionGenerator(baseCorpus, slotDict, tempDict);		
			for (QuestionIdDataset ds : testSets) {
				if (ds.datasetName.contains("dev")) {
					generateQuestions(ds.samples, ds.features, ds, model);
				}
			}
		}
		
		double[][] results = new double[testSets.size() + 1][];
		results[0] = predictAndEvaluate(trainSet, model, "");
		System.out.println(String.format("Training accuracy on %s:\t%s",
				trainSet.datasetName,
				StringUtils.doubleArrayToString("\t", results[0])));
		
		for (int i = 0; i < testSets.size(); i++) {
			QuestionIdDataset ds = testSets.get(i);
			results[i+1] = predictAndEvaluate(ds, model, "");
			System.out.println(String.format("Testing accuracy on %s:\t%s",
					ds.datasetName,
					StringUtils.doubleArrayToString("\t", results[i+1])));
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
		BaselineQuestionIdExperiment exp = null;
		String questionIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new BaselineQuestionIdExperiment(questionIdConfigPath);
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
			results[i] = exp.trainAndPredict(prm, false /* generate question */);
		}
		System.out.println("====== training finished =======");
		for (int i = 0; i < prms.size(); i++) {
			double[][] acc = results[i];
			System.out.println(prms.get(i).toString());
			System.out.println(String.format(
					"Training accuracy on %s:\t%s",
						exp.trainSet.datasetName,
						StringUtils.doubleArrayToString("\t", acc[0])));
			for (int j = 0; j < exp.testSets.size(); j++) {
				QuestionIdDataset ds = exp.testSets.get(j);
				System.out.println(String.format(
						"Testing accuracy on %s:\t%s",
							ds.datasetName,
							StringUtils.doubleArrayToString("\t", acc[j+1])));
			}
		}
	}
}