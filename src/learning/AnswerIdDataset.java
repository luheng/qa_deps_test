package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;

import data.Corpus;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

public class AnswerIdDataset extends QADataset {
	int[][] answerFlags, answerHeads;
	
	public AnswerIdDataset(Corpus corpus, String name) {
		super(corpus, name);
	}
	
	public int[][] getAnswerFlags() {
		return answerFlags;
	}
	
	public int[][] getAnswerHeads() {
		return answerHeads;
	}
	
	public void generateSamples(KBestParseRetriever syntaxHelper,
								boolean generateSpanBasedSamples) {
		int numPositiveSamples = 0;
		for (int i = 0; i < questions.size(); i++) {
			ArrayList<QASample> newSamples = generateSpanBasedSamples ? 
					syntaxHelper.generateSamplesFromSpans(questions.get(i),
							i /* question id */) :
					syntaxHelper.generateSamplesWithParses(questions.get(i),
							i /* question id */);				
			for (QASample sample : newSamples) {
				samples.add(sample);
				numPositiveSamples += (sample.isPositiveSample ? 1 : 0);
			}
			if (i % 100 == 99) {
				System.out.print(String.format(
						"Processed %d QAs, %d still left.\t",
						i + 1, questions.size() - i - 1));
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
			AnswerIdFeatureExtractor featureExtractor) {
		int numSamples = samples.size(),
			numQuestions = questions.size();
		features = new Feature[numSamples][];
		labels = new double[numSamples];
		answerFlags = new int[numQuestions][];
		answerHeads = new int[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			answerFlags[i] = questions.get(i).answerFlags;
			answerHeads[i] = new int[answerFlags.length];
			Arrays.fill(answerHeads[i], -1);
		}			
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
			answerHeads[sample.questionId][sample.answerWordPosition] = label;
			
			if (i % 10000 == 9999) {
				System.out.println(String.format(
						"Extracted features for %d samples, %d still left.",
						i + 1, numSamples - i - 1));
			}
		}

	}

}
