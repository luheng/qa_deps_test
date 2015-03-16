package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import util.Distribution;
import util.StringUtils;
import data.AnnotatedSentence;
import data.CountDictionary;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;
import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataAnalyzer {

	private static SRLCorpus corpus;
	private static ArrayList<CrowdFlowerQAResult> annotationResults;
	private static ArrayList<ArrayList<StructuredQAPair>> alignedQALists;
	private static ArrayList<Integer> sentenceIds;
	private static HashMap<Integer, ArrayList<Integer>> workerMap;
	private static HashMap<Integer, Integer> sentenceIdMap;
	private static HashMap<Integer, AnnotatedSentence> annotatedSentences;
	private static HashMap<Integer, HashMap<Integer, HashMap<String, Integer>>> agreedQAs;
	
	private static SRLAnnotationValidator validator = new SRLAnnotationValidator();
	
	public static void alignAnnotations() {
		alignedQALists = new ArrayList<ArrayList<StructuredQAPair>>();
		annotatedSentences = new HashMap<Integer, AnnotatedSentence>();
		sentenceIdMap = new HashMap<Integer, Integer>();
		sentenceIds = new ArrayList<Integer>();
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
				sentenceIds.add(sentId);
			}
		}
		
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
		agreedQAs = new HashMap<Integer,
				HashMap<Integer, HashMap<String, Integer>>>();
		for (AnnotatedSentence annotSent : annotatedSentences.values()) {
			int sentId = annotSent.sentence.sentenceID; 
			agreedQAs.put(sentId, new HashMap<Integer,
					HashMap<String, Integer>>());
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer> qaMap =
						new HashMap<String, Integer>();		
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encoded = qa.questionLabel;
					int k = (qaMap.containsKey(encoded) ?
							qaMap.get(encoded) : 0);
					qaMap.put(encoded, k + 1);
				}
				agreedQAs.get(sentId).put(propHead, qaMap);
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
		int numResults = annotationResults.size();
		// Get Gold SRL.
		String[][][] goldSRL = new String[sentenceIdMap.size()][][];
		for (int sentId : sentenceIdMap.keySet()) {
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			goldSRL[sentenceIdMap.get(sentId)] = validator.getGoldSRL(sentence);
		}
		
		// Flags
		int[] matchedCount = new int[numResults];
		int[] agreedCount = new int[numResults];
		Arrays.fill(matchedCount, 0);
		Arrays.fill(agreedCount, 0);
		
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
					String encoded = qa.questionLabel;
					if (agreedQAs.get(sentId).get(propHead).get(encoded) > 1) {
						agreed = true;
					}
				}
				matchedCount[r] += (matched ? 1 : 0);
				agreedCount[r] += (agreed ? 1 : 0);
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
				int goldCnt = matchedCount[r];
				numMatched.add(goldCnt);
				
				// Agreement with others
				int agreeCnt = agreedCount[r];
				
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
	
	private static void appendEdge(HashMap<Integer, ArrayList<Integer>> graph,
			int start, int end) {
		// FXIME: not efficient, change later.
		if (!graph.containsKey(start)) {
			graph.put(start, new ArrayList<Integer>());
		}
		if (!graph.get(start).contains(end)) {
			graph.get(start).add(end);
		}
	}
	
	private static void boostScores() {
		CountDictionary qaMap = new CountDictionary();
		HashMap<Integer, ArrayList<Integer>>
				worker2qa = new HashMap<Integer, ArrayList<Integer>>(),
				qa2worker = new HashMap<Integer, ArrayList<Integer>>();
		HashMap<Integer, Double> workerScores = new HashMap<Integer, Double>(),
								 qaScores = new HashMap<Integer, Double>();
		ArrayList<AnnotatedSentence> filteredSentences =
				new ArrayList<AnnotatedSentence>();
		for (int sentId : sentenceIds) {
			AnnotatedSentence
				annotSent = annotatedSentences.get(sentId),
				newSent = new AnnotatedSentence(annotSent.sentence);
			for (int propHead : annotSent.qaLists.keySet()) {
				newSent.addProposition(propHead);
			}
			filteredSentences.add(newSent);
		}
		
		double threshold = 0.6;
		
		for (int r = 0; r < annotationResults.size(); r++) {
			int workerId = annotationResults.get(r).cfWorkerId;
			for (StructuredQAPair qa : alignedQALists.get(r)) {
				int sentId = qa.sentence.sentenceID;
				int propHead = qa.propHead;
				String questionLabel = qa.questionLabel;
				//String answerStr = qa.getAnswerString();
				String qaStr = String.format("%d_%d_%s", sentId, propHead,
						questionLabel);//, answerStr);
				int qaId = qaMap.addString(qaStr);
				appendEdge(worker2qa, workerId, qaId);
				appendEdge(qa2worker, qaId, workerId);
			}
		}

		for (int workerId : worker2qa.keySet()) {
			workerScores.put(workerId, 0.5);
		}
		validator.ignoreLabels = true;
		validator.goldPropositionOnly = false;
		for (int t = 0; t < 10; t++) {
			// Update QA scores.
			for (int qaId : qa2worker.keySet()) {
				double score = 0.0;
				for (int workerId : qa2worker.get(qaId)) {
					score += workerScores.get(workerId);
				}
				qaScores.put(qaId, (score > threshold ? 1.0 : 0.0)); 
			}
			// Update worker scores.
			for (int workerId : worker2qa.keySet()) {
				double score = 0.0;
				for (int qaId : worker2qa.get(workerId)) {
					score += qaScores.get(qaId);
				}
				workerScores.put(workerId,
						score / worker2qa.get(workerId).size());
			}
			// Filter QAs and compute accuracy ..
			int qaCounter = 0;
			for (int i = 0; i < annotatedSentences.size(); i++) {
				AnnotatedSentence annotSent = annotatedSentences.get(
						sentenceIds.get(i));
				int sentId = annotSent.sentence.sentenceID;
				for (int prop : annotSent.qaLists.keySet()) {
					ArrayList<StructuredQAPair> newQAList =
							new ArrayList<StructuredQAPair>();
					HashSet<String> uniqueQAs = new HashSet<String>();
					
					for (StructuredQAPair qa : annotSent.qaLists.get(prop)) {
						String questionLabel = qa.questionLabel;
						String qaStr = String.format("%d_%d_%s", sentId, prop, questionLabel);
						String qaStr2 = questionLabel + "###" + qa.getAnswerString();
						
						int qaId = qaMap.addString(qaStr);
								
						if (qaScores.get(qaId) > threshold && !uniqueQAs.contains(qaStr2)) {
							newQAList.add(qa);
							uniqueQAs.add(qaStr2);
						}
					}
					filteredSentences.get(i).qaLists.put(prop, newQAList);
					qaCounter += newQAList.size();
				}
			}
			System.out.print("Num QA: " + qaCounter + "\t");
			validator.computeSRLAccuracy(filteredSentences, corpus);
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
	
	private static void encodeQuestions() {
		for (int r = 0; r < alignedQALists.size(); r++) {
			if (alignedQALists.get(r).size() == 0) {
				continue;
			}
			SRLSentence sentence = alignedQALists.get(r).get(0).sentence;
			String[][] gold = validator.getGoldSRL(sentence);
			
			for (StructuredQAPair qa : alignedQALists.get(r)) {
				String label = qa.questionLabel;
				int propHead = qa.propHead;
				for (int i = 0; i < sentence.length; i++) {
					if (!gold[propHead + 1][i + 1].isEmpty() &&
						validator.matchedGold(i, qa, sentence)) {
						String goldLabel = gold[propHead + 1][i + 1];
						if (goldLabel.equals("A0") && label.equals("what_0")) {
							System.out.println(qa.toString());
							System.out.println("Matched:\t" + goldLabel + "\t" + label);
						}
					}
				}
			}
		}
	}
	
	private static void questionSlotAnalysis() {
		CountDictionary whDist = new CountDictionary(),
					    auxDist = new CountDictionary(),
					    ph1Dist = new CountDictionary(),
					    trgDist = new CountDictionary(),
					    ph2Dist = new CountDictionary(),
					    ppDist = new CountDictionary(),
					    ph3Dist = new CountDictionary(),
					    verbDist = new CountDictionary();
		for (CrowdFlowerQAResult result : annotationResults) {
			for (String[] question : result.questions) {
				whDist.addString(question[0]);
				auxDist.addString(question[1]);
				ph1Dist.addString(question[2]);
				// TODO: transform target
				String t0 = "", t1 = "read";
				if (question[3].endsWith("ing")) {
					t1 = "reading";
				}
				String[] tmp = question[3].split(" ");
				if (tmp.length > 1) {
					for (int i = 0; i < tmp.length - 1; i++) {
						t0 += tmp[i] + " ";
					}
				}
				trgDist.addString(t0 + t1);
				verbDist.addString(question[1] + " " +  question[2] + " " + t0 + " " + t1);
				ph2Dist.addString(question[4]);
				ppDist.addString(question[5]);
				ph3Dist.addString(question[6]);
				
				if (question[1].equals("are") || question[1].equals("were")) {
					System.out.println(result.toString());
				}
			}
		}
		System.out.println("\nAUX");
		auxDist.prettyPrint();
		// Print distribution
		/*
		System.out.println("\nTRG");
		trgDist.prettyPrint();
		System.out.println("\nVERB");
		verbDist.prettyPrint();
		System.out.println("\nPH1");
		ph1Dist.prettyPrint(); 
		System.out.println("\nPH2");
		ph2Dist.prettyPrint();
		System.out.println("\nPH3");
		ph3Dist.prettyPrint();
		*/
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
		//alignAnnotations();
		//boostScores();
		//aggregateAnnotations();
		
		//printFeedback();
		//completionTimeAnalysis();
		
		// TODO: for each row in CFResults, label it with the following information:
		// (1) Averaged completion time.
		// (2) Averaged number of QAs
		// (3) Worker score - precision loss. If an answer does not match with an gold relation.
		// (4) Worker score - "eccentric" loss. If an answer does not match any other's answer span.
		
		//workerAnalysis();
		//encodeQuestions();
		
		questionSlotAnalysis();
	}
}
