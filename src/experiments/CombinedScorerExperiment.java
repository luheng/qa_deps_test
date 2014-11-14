package experiments;

import java.util.ArrayList;

import postprocess.AuxiliaryVerbPostprocessor;
import scorer.DistanceScorer;
import scorer.QuestionAnswerScorer;
import scorer.UniversalGrammarScorer;
import util.LatticeUtils;
import data.Accuracy;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.DepSentence;
import data.Evaluation;
import data.QAPair;
import decoding.Decoder;
import decoding.ViterbiDecoder;

/**
 * Test the different scorers.
 * @author luheng
 *
 */
public class CombinedScorerExperiment {

	private static DepCorpus trainCorpus = null;
	private static ArrayList<AnnotatedSentence> annotatedSentences = null;
	private static Decoder decoder = null;
	
	private static void testDistanceScorer() {
		DistanceScorer scorer = new DistanceScorer();
		Accuracy averagedAccuracy = new Accuracy();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length + 1;
			double[][] scores = new double[length][length];
			int[] parents = new int[length - 1];
			
			scorer.getScores(scores, depSentence);
			decoder.decode(scores, parents);
			Accuracy acc = Evaluation.getAccuracy(depSentence, parents);
			averagedAccuracy.add(acc);
		}
		System.out.println(
				String.format("Accuracy of distance scorer:\t%.2f",
						100.0 * averagedAccuracy.accuracy()));
	}
	
	private static void testQuestionAnswerScorer() {
		QuestionAnswerScorer scorer = new QuestionAnswerScorer();
		Accuracy averagedAccuracy =  new Accuracy();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length + 1;
			double[][] scores = new double[length][length];
			int[] parents = new int[length - 1];
			
			for (QAPair qa : sentence.qaList) {
				double[][] qaScores = new double[length][length];
				scorer.getScores(qaScores, depSentence, qa);
				LatticeUtils.addTo(scores, qaScores);
			}
			
			decoder.decode(scores, parents);
			Accuracy acc = Evaluation.getAccuracy(depSentence, parents);
			averagedAccuracy.add(acc);
		}
		System.out.println(
				String.format("Accuracy of question-answer scorer:\t%.2f",
				   100.0 * averagedAccuracy.accuracy()));
	}
	
	private static void testUniversalGrammarScorer() {
		UniversalGrammarScorer scorer = new UniversalGrammarScorer(trainCorpus);
		Accuracy averagedAccuracy = new Accuracy();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length + 1;
			double[][] scores = new double[length][length];
			int[] parents = new int[length - 1];
		
			scorer.getScores(scores, depSentence);
			
			decoder.decode(scores, parents);
			Accuracy acc = Evaluation.getAccuracy(depSentence, parents);
			averagedAccuracy.add(acc);
			/*
			 * For debugging.
			 * 
			if (length < 10) {
				// Print scores.
				for (int i = 0; i < scores.length; i++) {
					System.out.println(
							StringUtils.doubleArrayToString("\t", scores[i]));
				}
				System.out.println(StringUtils.join("\t",
						depSentence.corpus.wordDict.getStringArray(depSentence.tokens)));
				System.out.println(StringUtils.intArrayToString("\t", depSentence.parents));
				System.out.println(StringUtils.intArrayToString("\t", parents));
				System.out.println(sentence.toString());
			}
			*/
		}
		System.out.println(
				String.format("Accuracy of universal grammar scorer:\t%.2f",
				   100.0 * averagedAccuracy.accuracy()));	
	}
	
	private static void testCombinedScorer(double distWeight, double qaWeight,
										   double ugWeight) {
		DistanceScorer distScorer = new DistanceScorer();
		QuestionAnswerScorer qaScorer = new QuestionAnswerScorer();
		UniversalGrammarScorer ugScorer =
				new UniversalGrammarScorer(trainCorpus);
		AuxiliaryVerbPostprocessor fixer =
				new AuxiliaryVerbPostprocessor(trainCorpus);
		Accuracy averagedAccuracy = new Accuracy(),
				 averagedAccuracy2 = new Accuracy();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length + 1;
			double[][] scores = new double[length][length],
					   tempScores = new double[length][length];
						
			int[] parents = new int[length - 1],
				  fixedParents = new int[length - 1];
			
			LatticeUtils.fill(scores, 0.0);
			
			// Compute distance scores.
			distScorer.getScores(tempScores, depSentence);
			LatticeUtils.addTo(scores, tempScores, distWeight);
			// Compute QA scores.
			for (QAPair qa : sentence.qaList) {
				qaScorer.getScores(tempScores, depSentence, qa);
				LatticeUtils.addTo(scores, tempScores, qaWeight);
			}
			// Compute UG scores
			ugScorer.getScores(tempScores, depSentence);
			LatticeUtils.addTo(scores, tempScores, ugWeight);
			
			decoder.decode(scores, parents);
			Accuracy acc = Evaluation.getAccuracy(depSentence, parents);
			averagedAccuracy.add(acc);
			
			// Go through postprocessor.
			fixer.postprocess(fixedParents, parents, depSentence);
			Accuracy acc2 = Evaluation.getAccuracy(depSentence, fixedParents);
			averagedAccuracy2.add(acc2);
		}
		System.out.println(
				String.format("Combined accuracy:\t%.2f",
						   100.0 * averagedAccuracy.accuracy()));
		System.out.println(
				String.format("Combined accuracy with post-processing:\t%.2f",
				   			100.0 * averagedAccuracy2.accuracy()));
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		annotatedSentences = ExperimentUtils.loadAnnotatedSentences(trainCorpus);
		ExperimentUtils.doGreedyAlignment(annotatedSentences);
		decoder = new ViterbiDecoder();
		
		double avgSentenceLength = 0.0, avgNumQAs = 0.0;
		for (AnnotatedSentence sentence : annotatedSentences) {
			System.out.println(sentence.depSentence.sentenceID);
			avgSentenceLength += sentence.depSentence.length;
			avgNumQAs += sentence.qaList.size();
		}
		System.out.println("Averaged sentence length:\t" +
				avgSentenceLength / annotatedSentences.size());
		System.out.println("Averaged number of QAs:\t" +
				avgNumQAs / annotatedSentences.size());
		
		System.out.println("Dist");
		testCombinedScorer(1.0, 0.0, 0.0);
		System.out.println("QA");
		testCombinedScorer(0.0, 1.0, 0.0);
		System.out.println("UG");
		testCombinedScorer(0.0, 0.0, 1.0);
		System.out.println("Dist + QA");
		testCombinedScorer(1.0, 1.0, 0.0);
		System.out.println("Dist + UG");
		testCombinedScorer(1.0, 0.0, 1.0);
		System.out.println("QA + UG");
		testCombinedScorer(0.0, 1.0, 1.0);
		System.out.println("Dist + QA + UG");
		testCombinedScorer(1.0, 1.0, 1.0);
	}
}
 