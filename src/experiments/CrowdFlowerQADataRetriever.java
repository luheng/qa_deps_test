package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import data.AnnotatedSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import edu.stanford.nlp.util.StringUtils;
import annotation.CrowdFlowerQAResult;
import annotation.PropositionAligner;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataRetriever {

	private static final String annotationFilePath =
			//"crowdflower/CF_QA_trial_s20_result.csv";
			"crowdflower/cf_round1_100sents_259units/f680088_CF_QA_s100_0202_results.csv";
	
	public static void readAnnotationResult(
			ArrayList<CrowdFlowerQAResult> results) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		double avgTime = 0;
		int numRecords = 0;
		for (CSVRecord record : records) {
			CrowdFlowerQAResult qa = CrowdFlowerQAResult.parseCSV(record);
			results.add(qa);
			avgTime += qa.secondsToComplete;
			++ numRecords;
		}
		fileReader.close();
		
		avgTime /= numRecords;
		System.out.println(String.format("Read %d CrowdFlower Records.", numRecords));
		System.out.println(String.format("Averaged completion time: %.3f seconds.",
				avgTime));
		System.out.println(String.format("Averaged number of units per hour: %.3f",
				3600 / avgTime));
	}
	
	private static int getAlignmentDistance(int[] align1, int[] align2) {
		int a1 = align1[0], b1 = align1[align1.length - 1],
			a2 = align2[0], b2 = align2[align2.length - 1]; 
		return Math.min(
				Math.min(Math.abs(a1 - a2), Math.abs(a1 - b2)),
			    Math.min(Math.abs(b1 - a2), Math.abs(b1 - b2)));
	}
	
	/*
	private static boolean isBetterAnswer(int[][] answer1, int[][] answer2,
										  int propHead) {
		// Compare minimum distance to the proposition and span coverage;
		int minDist1 = Integer.MAX_VALUE, minDist2 = Integer.MAX_VALUE;
		for (int[] s1 : answer1) {
			minDist1 = Math.min(minDist1, Math.min(Math.abs(propHead - s1[0]),
										  		   Math.abs(propHead - s1[1])));
		}
		for (int[] s2 : answer2) {
			minDist2 = Math.min(minDist1, Math.min(Math.abs(propHead - s2[0]),
			  		   							   Math.abs(propHead - s2[1])));
		}
		if (minDist1 != minDist2) {
			return minDist1 < minDist2;
		}
	}
	*/
	
	private static void spanSweep(int[] flags, int[][] spans) {
		for (int[] span : spans) {
			for (int i = span[0]; i < span[1]; i++) {
				flags[i] += 1;
			}
		}
	}
	
	public static void alignAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerQAResult> cfResults, SRLCorpus corpus) {
		assert (annotatedSentences != null);
		PropositionAligner propAligner = new PropositionAligner();
		
		// Get a list of sentences.
		Set<Integer> sentenceIds = new HashSet<Integer>();
		HashMap<Integer, Integer> sentIdMap = new HashMap<Integer, Integer>();
		for (CrowdFlowerQAResult result : cfResults) {
			sentenceIds.add(result.sentenceId);
		}
		for (int id : sentenceIds) {
			annotatedSentences.add(new AnnotatedSentence(
					(SRLSentence) corpus.sentences.get(id)));
			sentIdMap.put(id, annotatedSentences.size() - 1);
		}
		
		for (CrowdFlowerQAResult result : cfResults) {
			int sentId = result.sentenceId;
			SRLSentence sentence = (SRLSentence) corpus.sentences
					.get(sentId);
			AnnotatedSentence currSent = annotatedSentences.get(
					sentIdMap.get(result.sentenceId));
			
			int propHead = -1;
			if (result.propStart == -1) {
				Proposition prop = propAligner.align(sentence, result.proposition);
				propHead = prop.span[1] - 1;
			} else {
				propHead = result.propEnd - 1;
			}
			currSent.addProposition(propHead);
			for (int i = 0; i < result.questions.size(); i++) {
				String[] question = result.questions.get(i);
				String[] answers = result.answers.get(i);	
				for (String answer : answers) {
					StructuredQAPair qa = new StructuredQAPair(sentence,
							propHead, question, answer, result);
					currSent.addQAPair(propHead, qa);
				}
			}
			/*
			if (!result.feedback.isEmpty()) {
				System.out.println(sentence.getTokensString());
				System.out.println("Prop:\t" + sentence.getTokenString(propHead));
				System.out.println("Feedback:\t" + result.feedback);
				for (int i = 0; i < result.questions.size(); i++) {
					System.out.println("\t" + StringUtils.join(result.questions.get(i), " "));
					System.out.println("\t" + StringUtils.join(result.answers.get(i), " / "));
				}
				System.out.println();
			}
			*/
		}
		
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, int[]> aggregatedQAMap =
						new HashMap<String, int[]>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedQuestion =
							qa.getNaiveQuestionEncoding();
					if (!aggregatedQAMap.containsKey(encodedQuestion)) {
						int[] answer = new int[annotSent.sentence.length];
						Arrays.fill(answer, 0);
						spanSweep(answer, qa.answerSpans);
						aggregatedQAMap.put(encodedQuestion, answer);
					} else {
						spanSweep(aggregatedQAMap.get(encodedQuestion),
								qa.answerSpans);
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		ArrayList<CrowdFlowerQAResult> annotationResults =
				new ArrayList<CrowdFlowerQAResult>();
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		
		try {
			readAnnotationResult(annotationResults);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		alignAnnotations(annotatedSentences, annotationResults, trainCorpus);
		/*
		for (AnnotatedSentence sent : annotatedSentences) {
			//System.out.println(sent.toString());
			for (int propId : sent.qaLists.keySet()) {
				System.out.println(sent.sentence.getTokenString(propId));
				ArrayList<StructuredQAPair> qaList = sent.qaLists.get(propId);
				for (StructuredQAPair qa : qaList) {
					System.out.println(qa.toString());
				}
			}
			System.out.println();
		}
		*/
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
