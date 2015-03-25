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

import util.LatticeUtils;
import util.StringUtils;
import data.AnnotatedSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.QAPair;
import annotation.AnnotationStage;
import annotation.CrowdFlowerQAResult;
import annotation.CrowdFlowerResult;
import annotation.CrowdFlowerStage2Result;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerStage2DataRetriever {

	private static final String qaFilePath =
			"crowdflower/cf_round1_100sents_259units/f680088_CF_QA_s100_final_results.csv";
	private static final String s2FilePath =
			"crowdflower/cf_round1_100sents_stage2/f705896_CF_QA_s100_stage2_testrun.csv";
	
	
	public static void readAnnotationResult(
			String annotationFilePath,
			ArrayList<CrowdFlowerResult> results,
			AnnotationStage stage) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		double avgTime = 0;
		int numRecords = 0;
		for (CSVRecord record : records) {
			CrowdFlowerResult result =
					(stage == AnnotationStage.QuestionAnswerStage) ?
						CrowdFlowerQAResult.parseCSV(record) :
						CrowdFlowerStage2Result.parseCSV(record);	
			results.add(result);
			avgTime += result.secondsToComplete;
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
	
	public static void alignAnnotations(
			HashMap<Integer, AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerResult> cfResults, SRLCorpus corpus) {
		assert (annotatedSentences != null);

		for (CrowdFlowerResult result : cfResults) {
			int sentId = result.getSentId(),
				propHead = result.getPropHead();
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			if (!annotatedSentences.containsKey(sentId)) {
				annotatedSentences.put(sentId, new AnnotatedSentence(sentence));
			}
			AnnotatedSentence currSent = annotatedSentences.get(sentId);
			currSent.addProposition(propHead);
			if (CrowdFlowerQAResult.class.isInstance(result)) {
				CrowdFlowerQAResult qaResult = (CrowdFlowerQAResult) result;
				for (int qid = 0; qid < qaResult.questions.size(); qid ++) {
					QAPair qa = new QAPair(sentence,
							propHead, qaResult.questions.get(qid),
							"" /* answer */, result);
					for (String answer : qaResult.answers.get(qid)) {
						qa.addAnswer(answer);
					}
					currSent.addQAPair(propHead, qa);
				}
			} else {
				CrowdFlowerStage2Result s2Result =
						(CrowdFlowerStage2Result) result;
				QAPair qa = new QAPair(sentence, propHead,
						s2Result.qlabel, "" /* answer */, result);
				qa.questionString = s2Result.question;
				for (String answer : s2Result.answers) {
					qa.addAnswer(answer);
				}
				currSent.addQAPair(propHead, qa);
			}
		}
	}
	
	public static void analyzeAnnotations(
			ArrayList<CrowdFlowerQAResult> stage1Results,
			ArrayList<CrowdFlowerStage2Result> stage2Results,
			SRLCorpus corpus) {
		// Aggregate Stage1 results.
		HashMap<String, ArrayList<CrowdFlowerQAResult>> stage1ResultsMap =
				new HashMap<String, ArrayList<CrowdFlowerQAResult>>();
		for (CrowdFlowerQAResult result : stage1Results) {
			int sentId = result.sentenceId,
				propHead = result.propEnd - 1;
			String cfKey = String.format("%d_%d", sentId, propHead);
			if (!stage1ResultsMap.containsKey(cfKey)) {
				stage1ResultsMap.put(cfKey, new ArrayList<CrowdFlowerQAResult>());
			}
			stage1ResultsMap.get(cfKey).add(result);
		}
		// Compare with Stage1 results.
		for (CrowdFlowerStage2Result result : stage2Results) {
			int sentId = result.sentenceId;
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			int propHead = result.propEnd - 1;
		
			// Retrieve Stage1 result
			String cfKey = String.format("%d_%d", sentId, propHead);
			
			for (CrowdFlowerQAResult stage1Result : stage1ResultsMap.get(cfKey)) {
				//TODO: debug the worker ID problem.
				System.out.println(result.stage1Id + "..." + stage1Result.cfWorkerId); 
				
				for (int qid = 0; qid < stage1Result.questions.size(); qid++) {
					String[] question = stage1Result.questions.get(qid);
					String qstr = StringUtils.join(" ", question).trim();
					if (!qstr.equalsIgnoreCase(result.question)) {
						continue;
					}
					// Compare.					
					System.out.println(qstr);
					System.out.println("==== stage1 answers ====");
					System.out.println(StringUtils.join("\n", stage1Result.answers.get(qid)));
					System.out.println("==== stage2 answers ====");
					System.out.println(StringUtils.join("\n", result.answers));
					System.out.println();
				}
			}
		}
	}
	
	private static void addAnswer(HashMap<String, int[]> qmap, String qlabel,
			int[] answerFlags) {
		if (!qmap.containsKey(qlabel)) {
			int[] flags = new int[answerFlags.length];
			Arrays.fill(flags, 0);
			qmap.put(qlabel, flags);
		}
		int[] flags = qmap.get(qlabel);
		for (int i = 0; i < answerFlags.length; i++) {
			flags[i] += answerFlags[i];
		}
	}
	
	private static void printAnswerTokens(SRLSentence sentence,
			int[] answerFlags) {
		for (int i = 0; i < answerFlags.length; i++) {
			if (answerFlags[i] > 0) {
				System.out.print(String.format("%s(%d) ",
						sentence.getTokenString(i), answerFlags[i]));
			}
		}
		System.out.println();
	}
	
	public static void aggregateAnnotations(
			HashMap<Integer, AnnotatedSentence> qaSents,
			HashMap<Integer, AnnotatedSentence> s2Sents,
			ArrayList<AnnotatedSentence> testSents) {
		
		int numQAs = 0,
			numAggregatedQAs = 0;
		
		for (int sentId : qaSents.keySet()) {
			AnnotatedSentence qaSent = qaSents.get(sentId);
			SRLSentence sent = (SRLSentence) qaSent.sentence;
			int s1Props = qaSent.qaLists.size(),
				s2Props = 0;
			
			for (int propHead : qaSent.qaLists.keySet()) {
				HashMap<String, int[]>
					s1QMap = new HashMap<String, int[]>(),
					s2QMap = new HashMap<String, int[]>();			 

				for (QAPair qa : qaSent.qaLists.get(propHead)) {
					addAnswer(s1QMap, qa.getQuestionLabel(), qa.answerFlags);
				}
				
				if (s2Sents.containsKey(sentId)) {
					AnnotatedSentence s2Sent = s2Sents.get(sentId);
					s2Props = s2Sent.qaLists.size();
					if (s2Sent.qaLists.containsKey(propHead)) {
						for (QAPair qa : s2Sent.qaLists.get(propHead)) {
							addAnswer(s2QMap, qa.getQuestionLabel(),
									qa.answerFlags);
						}
					}
				}
				
				ArrayList<QAPair> newList = new ArrayList<QAPair>();
				// Compute Stage1-Stage2 Agreement
				for (String qlabel : s1QMap.keySet()) {
					boolean disagree = false;
					int[] flags1 = s1QMap.get(qlabel);
					if (s2QMap.containsKey(qlabel)) {
						printAnswerTokens(sent, s1QMap.get(qlabel));
						printAnswerTokens(sent, s2QMap.get(qlabel));
						int[] flags2 = s2QMap.get(qlabel);
						int overlap = 0, all = 0, minS1 = 0;
						for (int i = 0; i < flags1.length; i++) {
							overlap += Math.min(flags1[i], flags2[i]);
							all += Math.max(flags1[i], flags2[i]);
							minS1 = Math.min(minS1, flags1[i]);
						}
						System.out.println(overlap + ", " + all);
						double overlapRatio = 1.0 * overlap / all;
						if (overlapRatio < 0.1 && minS1 <= 1) {
							disagree = true;
						}
					}
					if (!disagree) {
						QAPair newQA = new QAPair(qaSent.sentence, propHead,
							qlabel, "" /* answer */, null /* cfResult */);
						LatticeUtils.copy(newQA.answerFlags, flags1);
						newList.add(newQA);
					}
				}
				qaSent.qaLists.put(propHead, newList);
				if (s2Props == s1Props) {
					testSents.add(qaSent);
					numQAs += s1QMap.size();
					numAggregatedQAs += newList.size();
				}

			}
		}
		System.out.println(
				   "Num QAs before filtering:\t" + numQAs +
		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
		System.out.println("Number of test sentences\t" + testSents.size());
		
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		
		ArrayList<CrowdFlowerResult>
				qaResults = new ArrayList<CrowdFlowerResult>(),
				s2Results = new ArrayList<CrowdFlowerResult>();
		HashMap<Integer, AnnotatedSentence>
				qaSents = new HashMap<Integer, AnnotatedSentence>(),
				s2Sents = new HashMap<Integer, AnnotatedSentence>();
		
		try {
			System.out.println("\nReading Stage1 (Question Answering) Annotation Results");
			readAnnotationResult(qaFilePath, qaResults,
					AnnotationStage.QuestionAnswerStage);
			System.out.println("\nReading Stage2 (Answering) Annotation Results");
			readAnnotationResult(s2FilePath, s2Results,
					AnnotationStage.AnswerStage);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		alignAnnotations(qaSents, qaResults, trainCorpus);
		alignAnnotations(s2Sents, s2Results, trainCorpus);

		System.out.println(qaResults.size() + ", " + s2Results.size());
		System.out.println(qaSents.size() + ", " + s2Sents.size());
		
		ArrayList<AnnotatedSentence> testSents =
				new ArrayList<AnnotatedSentence>();
		
		aggregateAnnotations(qaSents, s2Sents, testSents);
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(testSents, trainCorpus);
	}
}
