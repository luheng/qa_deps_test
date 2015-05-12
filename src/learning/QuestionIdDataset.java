package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import annotation.QASlotPrepositions;
import annotation.QuestionEncoder;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

public class QuestionIdDataset {

	public String datasetName;
	Corpus corpus;
	public ArrayList<AnnotatedSentence> sentences;
	public ArrayList<QAPair> questions;
	public ArrayList<QASample> samples;
	public HashMap<Integer, Sentence> sentenceMap;
	public Feature[][] features;
	public double[] labels;
	
	public QuestionIdDataset(Corpus corpus, String name) {
		this(corpus);
		this.datasetName = name;
	}
	
	public QuestionIdDataset(Corpus corpus) {
		this.corpus = corpus;
		this.sentences = new ArrayList<AnnotatedSentence>();
		this.questions = new ArrayList<QAPair>();
		this.samples = new ArrayList<QASample>();
		this.sentenceMap = new HashMap<Integer, Sentence>();
	}
	
	public Collection<Integer> getSentenceIds() {
		return sentenceMap.keySet();
	}
	
	public Sentence getSentence(int sentId) {
		return sentenceMap.get(sentId);
	}

	public void loadData(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				new File(filePath)));
		String currLine = "";
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.trim().split("\t");
			assert (info.length == 2);
			currLine = reader.readLine();
			Sentence sent = corpus.addNewSentence(currLine);
			sent.source = info[0];
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
					QAPair qa = new QAPair(sent, propHead, j, question);
					String[] answers = info[8].split("###");
					for (String answer : answers) {
						qa.addAnswer(answer);
					}
					annotSent.addQAPair(propHead, qa);
					questions.add(qa);
				}
			}
			reader.readLine();
			sentences.add(annotSent);
		}
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.",
				sentences.size(), filePath));
		
		for (AnnotatedSentence sent : sentences) {
			sentenceMap.put(sent.sentence.sentenceID, sent.sentence);
		}
	}
	
	public static HashSet<String> getSlotLabels(Sentence sent) {
		HashSet<String> slots = new HashSet<String>();
		HashSet<String> ppOpts = getPPOptions(sent);
		String[] ph3Opts = new String[] {"someone", "something",
				"do something", "doing something" // "be something", "being something"
		};
		for (String ph : new String[] {"someone", "something"}) {
			slots.add("ARG_0" + "=" + ph);
			slots.add("ARG_1" + "=" + ph);
		}
		for (String ph : ph3Opts) {
			slots.add("ARG_2" + "=" + ph);
		}
		for (String pp : ppOpts) {
			for (String ph : ph3Opts) {
				slots.add("ARG_" + pp + "=" + ph);
			}
			for (String mod : new String[]{
					"WHERE", "WHEN", "WHY", "HOW", "HOW MUCH"}) {
				slots.add(mod + "_" + pp + "=.");
			}
		}
		return slots;
	}
	
	private static HashSet<String> getPPOptions(Sentence sent) {
		HashSet<String> opSet = new HashSet<String>();
		for (int i = 0; i < sent.length; i++) {
			String tok = sent.getTokenString(i).toLowerCase();
			if (QASlotPrepositions.ppSet.contains(tok)) {
				opSet.add(tok);
				if (i < sent.length - 1) {
					String tok2 = sent.getTokenString(i + 1).toLowerCase();
					if (QASlotPrepositions.ppSet.contains(tok2)) {
						opSet.add(tok + " " + tok2);
					}
				}
			}
		}
		for (String pp : QASlotPrepositions.mostFrequentPPs) {
			opSet.add(pp);
		}
		return opSet;
	}
	
	private HashSet<Integer> getNegativeLabels(Sentence sent,
			HashSet<Integer> posLabels, CountDictionary qlabelDict) {
		HashSet<Integer> negLabels = new HashSet<Integer>();
		for (int qid = 0; qid < qlabelDict.size(); qid++) {
			String[] info = qlabelDict.getString(qid).split("=")[0].split("_");
			String qsub = info.length > 1 ? info[1] : "";
			if (qsub.isEmpty() || getPPOptions(sent).contains(qsub)) {
				if (!posLabels.contains(qid)) {
					negLabels.add(qid);
				}
			}
		}
		return negLabels;
	}
	
	public void generateSamples(KBestParseRetriever syntaxHelper,
			CountDictionary qlabelDict) {
		// For each <sentence, target> pair, generate a set of samples
		int numTargetWords = 0, numPositiveSamples = 0;
		for (AnnotatedSentence annotSent : sentences) {
			Sentence sent = annotSent.sentence;
			for (int propHead : annotSent.qaLists.keySet()) {
				ArrayList<QAPair> qaList = annotSent.qaLists.get(propHead);
				CountDictionary slotDict = new CountDictionary();
				QuestionEncoder.encode(sent, propHead, qaList, slotDict, null);
				HashSet<Integer> qlabelIds = new HashSet<Integer>();
				for (String qlabel : slotDict.getStrings()) {
					qlabelIds.add(qlabelDict.lookupString(qlabel));
				}
				qlabelIds.remove(-1);
				ArrayList<QASample> newSamples =
						syntaxHelper.generateQuesitonIdSamples(
								sent,
								propHead,
								qlabelIds,
								getNegativeLabels(sent, qlabelIds, qlabelDict),
								qlabelDict);
				for (QASample sample : newSamples) {
					samples.add(sample);
					numPositiveSamples += (sample.isPositiveSample ? 1 : 0);
				}
			}
			if (numTargetWords++ % 100 == 99) {
				System.out.print(String.format(
						"Processed %d Sentences.\t",
						numTargetWords + 1));
				System.out.println(String.format(
						"Generated %d samples. %d positive, %d negative.",
						samples.size(), numPositiveSamples,
						samples.size() - numPositiveSamples));
			}
		}
		System.out.println(String.format(
				"Generated %d samples. %d positive, %d negative.",
				samples.size(), numPositiveSamples,
				samples.size() - numPositiveSamples));
	}
	
	public void loadSamples(String filePath)
			throws IOException, ClassNotFoundException {
		ObjectInputStream istream =
				new ObjectInputStream(new FileInputStream(filePath));
		int numPositiveSamples = 0;
		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) istream.readObject();
		for (int i = 0; i < objs.size(); i++) {
			QASample sample = (QASample) objs.get(i);
			samples.add(sample);
			numPositiveSamples += (sample.isPositiveSample ? 1 : 0);
		}
		istream.close();
		System.out.println(String.format(
				"Loaded %d samples from %s. %d positive, %d negative.",
				samples.size(), filePath, numPositiveSamples,
				samples.size() - numPositiveSamples));
	}
	
	public void extractFeaturesAndLabels(
			QuestionIdFeatureExtractor featureExtractor) {
		int numSamples = samples.size();
		features = new Feature[numSamples][];
		labels = new double[numSamples];
		for (int i = 0; i < numSamples; i++) {
			QASample sample = samples.get(i);
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			features[i] = new Feature[fv.size()];
			int[] fids = Arrays.copyOf(fv.keys(), fv.size());
			Arrays.sort(fids);
			for (int j = 0; j < fids.length; j++) {
				// Liblinear feature id starts from 1.
				features[i][j] = new FeatureNode(fids[j] + 1, fv.get(fids[j]));
			}
			int label = (sample.isPositiveSample ? 1 : -1);
			labels[i] = label;
			if (i % 10000 == 9999) {
				System.out.println(String.format(
						"Extracted features for %d samples, %d still left.",
						i + 1, numSamples - i - 1));
			}
		}

	}

}

