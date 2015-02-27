package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import util.Distribution;
import util.StringUtils;
import data.AnnotatedSentence;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataAnalyzer {

	private static SRLCorpus corpus;
	private static ArrayList<CrowdFlowerQAResult> annotationResults;
	private static ArrayList<ArrayList<StructuredQAPair>> alignedQALists;
	private static HashMap<Integer, ArrayList<Integer>> workerMap;
	private static HashMap<Integer, Integer> sentenceIdMap;
	private static HashMap<Integer, AnnotatedSentence> annotatedSentences;
	private static HashMap<Integer, HashMap<Integer, HashMap<String, Integer>>> agreedAnswers;
	
	private static SRLAnnotationValidator validator = new SRLAnnotationValidator();
	
	public static void alignAnnotations() {
		alignedQALists = new ArrayList<ArrayList<StructuredQAPair>>();
		annotatedSentences = new HashMap<Integer, AnnotatedSentence>();
		
		for (CrowdFlowerQAResult result : annotationResults) {
			int sentId = result.sentenceId;
			int propHead = result.propEnd - 1;
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			ArrayList<StructuredQAPair> qaList =
					new ArrayList<StructuredQAPair>();
			
			if (!annotatedSentences.containsKey(sentId)) {
				annotatedSentences.put(sentId, new AnnotatedSentence(sentence));
			}
			annotatedSentences.get(sentId).addProposition(propHead);
			
			for (int i = 0; i < result.questions.size(); i++) {
				String[] question = result.questions.get(i);				
				if (question[0].equalsIgnoreCase("how many")) {
					continue;
				}
				String[] answers = result.answers.get(i);
				for (String answer : answers) {
					StructuredQAPair qa = new StructuredQAPair(sentence,
							propHead, question, answer, result);
					qaList.add(qa);
					annotatedSentences.get(sentId).addQAPair(propHead, qa);
				}
			}
			alignedQALists.add(qaList);
		}
	}
	
	static void aggregateAnnotations() {
		agreedAnswers = new HashMap<Integer,
				HashMap<Integer, HashMap<String, Integer>>>();
		for (AnnotatedSentence annotSent : annotatedSentences.values()) {
			int sentId = annotSent.sentence.sentenceID; 
			agreedAnswers.put(sentId, new HashMap<Integer,
					HashMap<String, Integer>>());
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer> answerMap =
						new HashMap<String, Integer>();		
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedAnswer = qa.getAnswerString();
					int k = (answerMap.containsKey(encodedAnswer) ?
							answerMap.get(encodedAnswer) : 0);
					answerMap.put(encodedAnswer, k + 1);
				}
				agreedAnswers.get(sentId).put(propHead, answerMap);
			}
		}
	}
	
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
		sentenceIdMap = new HashMap<Integer, Integer>();
		workerMap = new HashMap<Integer, ArrayList<Integer>>();
		
		int numResults = annotationResults.size();
		
		for (int i = 0; i < numResults; i++) {
			CrowdFlowerQAResult result = annotationResults.get(i);
			int workerId = result.cfWorkerId;
			if (!workerMap.containsKey(workerId)) {
				workerMap.put(workerId, new ArrayList<Integer>());
			}
			workerMap.get(workerId).add(i);
			
			int sentId = result.sentenceId;
			if (!sentenceIdMap.containsKey(sentId)) {
				int mappedId = sentenceIdMap.size();
				sentenceIdMap.put(sentId, mappedId);
			}
		}
		
		// Get Gold SRL.
		String[][][] goldSRL = new String[sentenceIdMap.size()][][];
		for (int sentId : sentenceIdMap.keySet()) {
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			goldSRL[sentenceIdMap.get(sentId)] = validator.getGoldSRL(sentence);
		}
		
		// Flags
		int[] matchedGold = new int[numResults];
		int[] agreedAnswer = new int[numResults];
		Arrays.fill(matchedGold, 0);
		Arrays.fill(agreedAnswer, 0);
		
		for (int r = 0; r < numResults; r++) {
			// Match with gold SRL
			CrowdFlowerQAResult result = annotationResults.get(r);
			int sentId = result.sentenceId;
			int propHead = result.propEnd - 1;
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			
			ArrayList<StructuredQAPair> qaList = alignedQALists.get(r);
			String[][] gold = goldSRL[sentenceIdMap.get(sentId)];
			
			for (StructuredQAPair qa : qaList) {
				boolean matched = false,
						agreed = false;
				for (int argHead = 0; argHead < sentence.length; argHead++) {
					if (gold[propHead + 1][argHead + 1].isEmpty()) {
						continue;
					}
					if (validator.matchedGold(argHead, qa, sentence)) {
						matched = true;
					}
				
					if (agreedAnswers.get(sentId).get(propHead).get(
							qa.getAnswerString()) > 1) {
						agreed = true;
					}
				}
				matchedGold[r] += (matched ? 1 : 0);
				agreedAnswer[r] += (agreed ? 1 : 0);
			}
		}
		
		// Analysis 1: completion time
		//          2: number of QAs (per sentence)
		System.out.println("Worker\tNum rows\tAvg time\tMin time\tMax time\tAvg num QAs" +
				"\tMin Matched\tMax Matched\tAvg Matched\tAvg Prec\tPrec Std" +
				"\tAvg Agreement\tAgreement Std");
		
		for (int workerId : workerMap.keySet()) {
			ArrayList<Integer> resultIds = workerMap.get(workerId);
			Distribution compTime = new Distribution(),
					     numQAs = new Distribution(),
					     numMatched = new Distribution(),
					     precision = new Distribution(),
					     agreement = new Distribution();
			
			for (int r : resultIds) {
				CrowdFlowerQAResult result = annotationResults.get(r);
				// Completion time.
				compTime.add(result.secondsToComplete);
				
				// Number of QAs
				// TODO: count number of different of answers too.
				int qaCnt = alignedQALists.get(r).size();
				numQAs.add(qaCnt);
				
				// Matched gold
				int goldCnt = matchedGold[r];
				numMatched.add(goldCnt);
				
				// Agreement with others
				int agreeCnt = agreedAnswer[r];
				
				precision.add(1.0 * goldCnt / qaCnt);
				agreement.add(1.0 * agreeCnt / qaCnt);
			}
			
			System.out.println(String.format("%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f",
					workerId, compTime.getNumSamples(),
					compTime.getMean(), compTime.getMin(), compTime.getMax(),
					numQAs.getMean(),
					(int) numMatched.getMin(), (int) numMatched.getMax(), numMatched.getMean(),
					precision.getMean(), precision.getStd(),
					agreement.getMean(), agreement.getStd()));
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
	
	public static void main(String[] args) {
		corpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		annotationResults = new ArrayList<CrowdFlowerQAResult>();
		try {
			CrowdFlowerQADataRetriever.readAnnotationResult(annotationResults);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		alignAnnotations();
		aggregateAnnotations();
		
		//printFeedback();
		//completionTimeAnalysis();
		
		// TODO: for each row in CFResults, label it with the following information:
		// (1) Averaged completion time.
		// (2) Averaged number of QAs
		// (3) Worker score - precision loss. If an answer does not match with an gold relation.
		// (4) Worker score - "eccentric" loss. If an answer does not match any other's answer span.
		
		workerAnalysis();
	}
}
