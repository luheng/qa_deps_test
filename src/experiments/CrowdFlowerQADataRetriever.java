package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import annotation.CrowdFlowerQAResult;
import annotation.PropositionAligner;
import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataRetriever {

	private static final String annotationFilePath =
			//"crowdflower/CF_QA_trial_s20_result.csv";
			"crowdflower/cf_round1_100sents_259units/f680088_CF_QA_s100_final_results.csv";
	
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
	
	// Find all spans
	// FIXME: this might be problematic
	private static int[][] getSpans(int[] flags) {
		ArrayList<int[]> spanBuffer = new ArrayList<int[]>();
		int start = -1;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] > 0 && start == -1) {
				start = i;
			}
			if (flags[i] == 0 && start != -1) {
				spanBuffer.add(new int[] {start, i});
				start = -1;
			}
		}
		if (start != -1) {
			spanBuffer.add(new int[] {start, flags.length});
		}
		int[][] spans = new int[spanBuffer.size()][2];
		for (int i = 0; i < spanBuffer.size(); i++) {
			spans[i][0] = spanBuffer.get(i)[0];
			spans[i][1] = spanBuffer.get(i)[1];
		}
		return spans;
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
				if (question[0].equalsIgnoreCase("how many")) {
					continue;
				}
				String[] answers = result.answers.get(i);
				for (String answer : answers) {
					StructuredQAPair qa = new StructuredQAPair(sentence,
							propHead, question, answer, result);
					currSent.addQAPair(propHead, qa);
				}
			}
			// TODO: look at people's feedback
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
	}
	
	static void aggregateAnnotationsByAnswer(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		int numQAs = 0,
			numAggregatedQAs = 0;

		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer> aggregatedQAMap =
						new HashMap<String, Integer>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedAnswer = qa.getAnswerString();
					if (!aggregatedQAMap.containsKey(encodedAnswer)) {
						aggregatedQAMap.put(encodedAnswer, 1);
					} else {
						int k = aggregatedQAMap.get(encodedAnswer);
						aggregatedQAMap.put(encodedAnswer, k + 1);
					}
				}
				ArrayList<StructuredQAPair> agreedList =
						new ArrayList<StructuredQAPair>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedAnswer = qa.getAnswerString();
					if (aggregatedQAMap.get(encodedAnswer) > 1) {
						agreedList.add(qa);
						aggregatedQAMap.put(encodedAnswer, 0);
					}
				}
				numQAs += annotSent.qaLists.get(propHead).size();
				numAggregatedQAs += agreedList.size();
				annotSent.qaLists.put(propHead, agreedList);
			}
		}
		System.out.println("Num QAs before filtering:\t" + numQAs +
		 		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
	}
	
	static void aggregateAnnotationsByQuestion(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		int numQAs = 0,
			numAggregatedQAs = 0;

		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer>
					questionMap = new HashMap<String, Integer>(),
					qaMap = new HashMap<String, Integer>();
				
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedQuestion =
							QuestionEncoder.encode(qa.questionWords);	
					if (!questionMap.containsKey(encodedQuestion)) {
						questionMap.put(encodedQuestion, 1);
					} else {
						int k = questionMap.get(encodedQuestion);
						questionMap.put(encodedQuestion, k + 1);
					}
				}
				ArrayList<StructuredQAPair> agreedList =
						new ArrayList<StructuredQAPair>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedQuestion =
							QuestionEncoder.encode(qa.questionWords);
					if (encodedQuestion.contains("???")) {
						continue;
					}
					String encodedAnswer = qa.getAnswerString();
					String encodedQA = encodedQuestion + "###" + encodedAnswer;
					
					if (questionMap.get(encodedQuestion) > 1 &&
						!qaMap.containsKey(encodedQA)) {
						agreedList.add(qa);
						qaMap.put(encodedQA, 1);
					}
				}
				numQAs += annotSent.qaLists.get(propHead).size();
				numAggregatedQAs += agreedList.size();
				annotSent.qaLists.put(propHead, agreedList);
			}
		}
		System.out.println("Num QAs before filtering:\t" + numQAs +
		 		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
	}
	
	static void aggregateAnnotationsByQA(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		int numQAs = 0,
			numAggregatedQAs = 0;
		
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer>
					questionMap = new HashMap<String, Integer>(),
					answerMap = new HashMap<String, Integer>(),
					qaMap = new HashMap<String, Integer>();
		
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedQuestion =
							QuestionEncoder.encode(qa.questionWords);
					String encodedAnswer = qa.getAnswerString();
					
					if (!questionMap.containsKey(encodedQuestion)) {
						questionMap.put(encodedQuestion, 1);
					} else {
						int k = questionMap.get(encodedQuestion);
						questionMap.put(encodedQuestion, k + 1);
					}
					
					if (!answerMap.containsKey(encodedAnswer)) {
						answerMap.put(encodedAnswer, 1);
					} else {
						int k = answerMap.get(encodedAnswer);
						answerMap.put(encodedAnswer, k + 1);
					}
				}
				ArrayList<StructuredQAPair> agreedList =
						new ArrayList<StructuredQAPair>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedQuestion =
							QuestionEncoder.encode(qa.questionWords);
					String encodedAnswer = qa.getAnswerString();
					String encodedQA = encodedQuestion + "###" + encodedAnswer;
					
					if ((questionMap.get(encodedQuestion) > 1 ||
						answerMap.get(encodedAnswer) > 1) &&
						!qaMap.containsKey(encodedQA)) {
						agreedList.add(qa);
						qaMap.put(encodedQA, 1);
					}
				}
				numQAs += annotSent.qaLists.get(propHead).size();
				numAggregatedQAs += agreedList.size();
				annotSent.qaLists.put(propHead, agreedList);
			}
		}
		
		System.out.println("Num QAs before filtering:\t" + numQAs +
				 		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
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
		//aggregateAnnotationsByQuestion(annotatedSentences);
		//aggregateAnnotationsByAnswer(annotatedSentences);
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
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
