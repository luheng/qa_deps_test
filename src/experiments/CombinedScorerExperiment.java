package experiments;

import java.util.ArrayList;

import constraints.ConstraintChecker;
import postprocess.AbstractPostprocessor;
import postprocess.AuxiliaryVerbPostprocessor;
import postprocess.FlatNounPhrasePostprocessor;
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
	
	public static void testSentence(AnnotatedSentence sentence,
									double distWeight,
									double qaWeight,
									double ugWeight,
									boolean printAnalysis) {
		DepSentence depSentence = sentence.depSentence;
		trainCorpus = depSentence.corpus;
		int length = depSentence.length + 1;
		
		DistanceScorer distScorer = new DistanceScorer();
		QuestionAnswerScorer qaScorer = new QuestionAnswerScorer();
		UniversalGrammarScorer ugScorer =
				new UniversalGrammarScorer(trainCorpus);
		
		AbstractPostprocessor verbFixer =
				new AuxiliaryVerbPostprocessor(trainCorpus);
		AbstractPostprocessor npFixer =
				new FlatNounPhrasePostprocessor(trainCorpus);
		
		decoder = new ViterbiDecoder();
		
		
		double[][] scores = new double[length][length],
				   tempScores = new double[length][length];
					
		int[] parents = new int[length - 1],
			  fixedParents = new int[length - 1],
			  fixedParents2 = new int[length - 1];
		
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
		
		// Go through post-processor.
		verbFixer.postprocess(fixedParents, parents, depSentence);
		npFixer.postprocess(fixedParents2, fixedParents, depSentence);
		Accuracy acc2 = Evaluation.getAccuracy(depSentence, fixedParents2);
		
		// Print out analysis
		if (printAnalysis) {
			depSentence.prettyPrintDebugString(fixedParents2, scores);
			/*
			depSentence.prettyPrintJSONDebugString(fixedParents2);
			System.out.println();
			*/
		}		
		System.out.println(
				String.format("Acc: %.2f%%, after post-processing: %.2f%%",
							100.0 * acc.accuracy(), 100.0 * acc2.accuracy()));
	}
	
	private static void testCombinedScorer(double distWeight, double qaWeight,
										   double ugWeight) {
		DistanceScorer distScorer = new DistanceScorer();
		QuestionAnswerScorer qaScorer = new QuestionAnswerScorer();
		UniversalGrammarScorer ugScorer =
				new UniversalGrammarScorer(trainCorpus);
		
		AbstractPostprocessor verbFixer =
				new AuxiliaryVerbPostprocessor(trainCorpus);
		AbstractPostprocessor npFixer =
				new FlatNounPhrasePostprocessor(trainCorpus); 
		Accuracy averagedAccuracy = new Accuracy(),
				 averagedAccuracy2 = new Accuracy();
		
		int numConstraintViolation = 0, numGoldViolation = 0, totalNumQA = 0;
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length + 1;
			double[][] scores = new double[length][length],
					   tempScores = new double[length][length];
						
			int[] parents = new int[length - 1],
				  fixedParents = new int[length - 1],
				  fixedParents2 = new int[length - 1];
			
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
			
			// Go through post-processor.
			verbFixer.postprocess(fixedParents, parents, depSentence);
			npFixer.postprocess(fixedParents2, fixedParents, depSentence);
			Accuracy acc2 = Evaluation.getAccuracy(depSentence, fixedParents2);
			averagedAccuracy2.add(acc2);
			
			// Print out analysis
			System.out.println(String.format("ID: %d\tAccuracy: %.2f",
					depSentence.sentenceID, 100.0 * acc2.accuracy()));
			depSentence.prettyPrintDebugString(fixedParents2, scores);
			//depSentence.prettyPrintDebugString(parents, scores);
			
			/*
			depSentence.prettyPrintJSONDebugString(fixedParents2);
			System.out.println();
			*/
			
			// Check constraint violation
			for (QAPair qa : sentence.qaList) {
				boolean goldViolation = !ConstraintChecker.check(
						sentence.depSentence, qa, sentence.depSentence.parents);
				if (goldViolation) {
					numGoldViolation ++;
				}
				if (!ConstraintChecker.check(sentence.depSentence, qa,
											 fixedParents2)) {
					numConstraintViolation ++;
					if (!goldViolation) {
						//System.out.println("Constraint Violation::\t" +
						//		qa.toString());
					}
				}
				
			}
			totalNumQA += sentence.qaList.size();
		}
		System.out.println(
				String.format("Combined accuracy:\t%.2f",
						   100.0 * averagedAccuracy.accuracy()));
		System.out.println(
				String.format("Combined accuracy with post-processing:\t%.2f",
				   			100.0 * averagedAccuracy2.accuracy()));
		System.out.println(
				String.format("Violated constraints: %d(%d)\t",
						numConstraintViolation, totalNumQA));
		System.out.println(
				String.format("Gold violated constraints: %d(%d)\t",
						numGoldViolation, totalNumQA));
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		if (ExperimentUtils.useNumberedAnnotation) {
			annotatedSentences = ExperimentUtils.loadNumberedAnnotation(trainCorpus);
		} else {
			annotatedSentences = ExperimentUtils.loadAnnotatedSentences(trainCorpus);
			ExperimentUtils.doGreedyAlignment(annotatedSentences);
		}		
		decoder = new ViterbiDecoder();
		
		// For debugging: print out loaded QA pairs.
		for (AnnotatedSentence sentence : annotatedSentences) {
			System.out.println(sentence.toString());
		}
		
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
		
		/*
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
		*/
		
		System.out.println("QA + UG");
		testCombinedScorer(0.0, 1.0, 1.0);
		/*
		System.out.println("Dist + QA + UG");
		testCombinedScorer(1.0, 1.0, 1.0);
		*/
	
	}
}
 