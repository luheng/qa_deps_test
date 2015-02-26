package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import util.Distribution;
import util.StringUtils;
import data.AnnotatedSentence;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;

public class CrowdFlowerQADataAnalyzer {

	private static SRLCorpus corpus;
	private static ArrayList<CrowdFlowerQAResult> annotationResults;
	private static ArrayList<AnnotatedSentence> annotatedSentences;
	
	private static void completionTimeAnalysis() {
		double avgTime = 0, minTime = 1e8, maxTime = 0;
		
		for (CrowdFlowerQAResult result : annotationResults) {
			double secs = result.secondsToComplete;
			avgTime += secs;
			minTime = Math.min(minTime, secs);
			maxTime = Math.max(maxTime, secs);
		}
		
		System.out.println("Averaged: " + avgTime / annotationResults.size() + "\n" +
						   "Minimum: " + minTime + "\n" +
						   "Maximum: " + maxTime);
		
		// TODO
		// Distribution of completion time
	}
	
	private static void workerAnalysis() {
		HashMap<Integer, ArrayList<CrowdFlowerQAResult>> workerMap =
				new HashMap<Integer, ArrayList<CrowdFlowerQAResult>>();
		
		for (CrowdFlowerQAResult result : annotationResults) {
			int workerId = result.cfWorkerId;
			if (!workerMap.containsKey(workerId)) {
				workerMap.put(workerId, new ArrayList<CrowdFlowerQAResult>());
			}
			workerMap.get(workerId).add(result);
		}
		
		// Analysis 1: completion time
		//          2: number of QAs (per sentence)
		System.out.println("Worker\tNum rows\tAvg time\tMin time\tMax time\tAvg num QAs");
		
		for (int workerId : workerMap.keySet()) {
			ArrayList<CrowdFlowerQAResult> results = workerMap.get(workerId);
			HashMap<Integer, Integer> qaCounter = new HashMap<Integer, Integer>();
			Distribution compTime = new Distribution(),
					     numQAs = new Distribution();
			
			for (CrowdFlowerQAResult result : results) {
				
				// Completion time.
				compTime.add(result.secondsToComplete);
				
				// Number of QAs
				int cnt = qaCounter.containsKey(result.sentenceId) ?
						qaCounter.get(result.sentenceId) : 0;
				qaCounter.put(result.sentenceId, cnt + 1);
			}
			
			for (int sentId : qaCounter.keySet()) {
				numQAs.add(qaCounter.get(sentId));
			}
			
			System.out.println(String.format("%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f",
					workerId, compTime.getNumSamples(), compTime.getMean(),
					compTime.getMin(), compTime.getMax(),
					numQAs.getMean()));
		}	
	}
	
	private static void printFeedback() {
		for (CrowdFlowerQAResult result : annotationResults) {
			if (!result.feedback.isEmpty()) {
				SRLSentence sentence = (SRLSentence)
						corpus.sentences.get(result.sentenceId);
				System.out.println("Proposition:\t" + result.proposition);
				for (int i = 0; i < result.questions.size(); i++) {
					System.out.println("\t" + StringUtils.join(" ", result.questions.get(i)));
					System.out.println("\t\t" + StringUtils.join(" ", result.answers.get(i)));
				}
				System.out.println(sentence.toString());
				System.out.println(result.feedback + "\n");
			}
		}
	}
	
	//private static void 
	
	public static void main(String[] args) {
		corpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		annotationResults = new ArrayList<CrowdFlowerQAResult>();
		annotatedSentences = new ArrayList<AnnotatedSentence>();
		
		try {
			CrowdFlowerQADataRetriever.readAnnotationResult(annotationResults);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		CrowdFlowerQADataRetriever.alignAnnotations(annotatedSentences,
				annotationResults, corpus);
		//aggregateAnnotations(annotatedSentences);
		
		//printFeedback();
		//completionTimeAnalysis();
		workerAnalysis();
	}
}
