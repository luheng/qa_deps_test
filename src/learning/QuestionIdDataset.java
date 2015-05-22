package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
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

public class QuestionIdDataset extends QADataset {
	
	public QuestionIdDataset(Corpus corpus, String name) {
		super(corpus, name);
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
			CountDictionary qlabelDict, boolean aggregateLabels) {
		// For each <sentence, target> pair, generate a set of samples
		int numTargetWords = 0, numPositiveSamples = 0;
		for (AnnotatedSentence annotSent : sentences) {
			Sentence sent = annotSent.sentence;
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, String> slots = new HashMap<String, String>();
				HashSet<Integer> qlabelIds = new HashSet<Integer>();
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					for (String lb : QuestionEncoder.getLabels(qa.questionWords)) {
						if (!lb.contains("=")) {
							continue;
						}
						String pfx = lb.split("=")[0];
						String val = lb.split("=")[1];
						if (slots.containsKey(pfx) && !slots.get(pfx).equals(val)) {
							slots.put(pfx, "something");
						} else {
							slots.put(pfx, val);
						}
					}
				}
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String lb = QuestionEncoder.getLabels(qa.questionWords)[0];
					String pfx = lb.split("=")[0];
					String val = lb.split("=")[1];
					if (!aggregateLabels || slots.get(pfx).equals(val)) {
						qlabelIds.add(qlabelDict.lookupString(lb));
						continue;
					}
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
	
	private static TIntDoubleHashMap normalizeFeatures(TIntDoubleHashMap fv) {
		TIntDoubleHashMap newFv = new TIntDoubleHashMap();
		double l2Norm = 0.0;
		for (double v : fv.values()) {
			l2Norm += v * v;
		}
		l2Norm = Math.sqrt(l2Norm);
		for (int k : fv.keys()) {
			newFv.put(k, fv.get(k) / l2Norm);
		}
		return newFv;
	}
	
	public void extractFeaturesAndLabels(
			QuestionIdFeatureExtractor featureExtractor, boolean normalize) {
		int numSamples = samples.size();
		features = new Feature[numSamples][];
		labels = new double[numSamples];
		for (int i = 0; i < numSamples; i++) {
			QASample sample = samples.get(i);
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			if (normalize) {
				fv = normalizeFeatures(fv);
			}
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

