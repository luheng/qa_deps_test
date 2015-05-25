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
import learning.QuestionIdDataset;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import evaluation.F1Metric;

public class PopularityQuestionIdExperiment {
	private QuestionIdConfig config;
	private Corpus baseCorpus; 
	private QuestionIdDataset trainSet;
	private ArrayList<QuestionIdDataset> testSets;
	private CountDictionary labelDict, tempDict;
	private QuestionGenerator qgen;
	
	private HashMap<Integer, Double> popScores;
	private ArrayList<Double> sortedScores;
	
	private String getSampleFileName(QuestionIdDataset ds) {
		return ds.datasetName + ".qgen.k" + config.kBest + ".smp";
	}
	
	private String getTemplateString(String[] temp) {
		String[] shortTemp = new String[temp.length];
		for (int i = 0; i < temp.length; i++) {
			String s = temp[i].split("=")[0];
			shortTemp[i] = s.contains("_") ? s.split("_")[0] + "_PP" : s;
		}
		return StrUtils.join("\t", "_", shortTemp);
	}
	
	public PopularityQuestionIdExperiment(String questionIdConfigPath)
			throws IOException {
		config = questionIdConfigPath.isEmpty() ? new QuestionIdConfig():
					new QuestionIdConfig(questionIdConfigPath);
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new ArrayList<QuestionIdDataset>();
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
		labelDict = new CountDictionary();
		tempDict = new CountDictionary();
		int numVerbs = 0;
		for (AnnotatedSentence sent : trainSet.sentences) {
			numVerbs += sent.qaLists.size();
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
		popScores = new HashMap<Integer, Double>();
		sortedScores = new ArrayList<Double>();
		for (int i = 0; i < labelDict.size(); i++) {
			double sc = 1.0 * labelDict.getCount(i) / numVerbs;
			popScores.put(i, sc);
			sortedScores.add(sc);
			System.out.println(i + "\t" + labelDict.getString(i) + "\t" + sc);
		}
		Collections.sort(sortedScores, Collections.reverseOrder());
		
		if (config.regenerateSamples) {
			System.out.println("Error: Generate training samples first.");
			return;
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
	}
	
	public double[][][] predict() {
		double[][][] results = new double[testSets.size()][][];
		for (int d = 0; d < testSets.size(); d++) {
			QuestionIdDataset ds = testSets.get(d);
			if (config.evalThreshold > 0) {
				results[d] = new double[config.numPRCurvePoints][];
				for (int k = 0; k < config.numPRCurvePoints; k++) {
					double thr = 1.0 * k / config.numPRCurvePoints;
					results[d][k] = predictAndEvaluate(
							ds, thr, -1, "", "");
				}
			} else {
				results[d] = new double[labelDict.size()][];
				for (int k = 1; k <= labelDict.size(); k++) {
					results[d][k-1] = predictAndEvaluate(
							ds, -1.0, k, "", "");
				}
			}
		}
		return results;
	}
	
	private double[] predictAndEvaluate(
			QuestionIdDataset ds,
			double evalThreshold,
			int evalTopK,
			String qgenPath,
			String debugPath) {
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
		HashSet<String> sl = new HashSet<String>();
		double thr = evalThreshold;
		if (thr < 0) {
			thr = sortedScores.get(Math.min(sortedScores.size(), evalTopK) - 1);
		}
		for (int i = 0; i < labelDict.size(); i++) {
			if (popScores.get(i) >= thr) {
				sl.add(labelDict.getString(i));
			}
		}
		F1Metric microF1 = new F1Metric();
		double macroPrec = .0, macroRecall = .0;
		int cnt = 0;
		for (AnnotatedSentence sent : ds.sentences) {
			int sid = sent.sentence.sentenceID;
			for (int pid : sent.qaLists.keySet()) {
				HashSet<String> gl = ds.goldLabels.get(sid).get(pid);
				HashMap<String, Double> labels = new HashMap<String, Double>();
				for (String lb : sl) {
					labels.put(lb, 1.0);
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
				
				/************ Print debugging info ***************/
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
							debugWriter.write(String.format("%s\t", glb));
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
		PopularityQuestionIdExperiment exp = null;
		String questionIdConfigPath = args.length > 0 ? args[0] : "";
		try {
			exp = new PopularityQuestionIdExperiment(questionIdConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		double[][][] results = exp.predict();
		for (int j = 0; j < exp.testSets.size(); j++) {
			QuestionIdDataset ds = exp.testSets.get(j);
			System.out.println(String.format(
					"Testing accuracy on %s", ds.datasetName));
			for (int k = 0; k < results[j].length; k++) {
				System.out.println(k + "\t" +
					StrUtils.doubleArrayToString("\t", results[j][k]));
			}
		}
	}
}