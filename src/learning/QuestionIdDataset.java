package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
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

public class QuestionIdDataset extends QADataset {
	
	public QuestionIdDataset(Corpus corpus, String name) {
		super(corpus, name);
	}
	
	@SuppressWarnings("unused")
	private static HashSet<String> getSlotLabels(Sentence sent) {
		HashSet<String> slots = new HashSet<String>();
		HashSet<String> ppOpts = getPPOptions(sent);
		String[] ph3Opts = new String[] {
				"someone", "something",
				"do something", "doing something",
				"be something", "being something"
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

