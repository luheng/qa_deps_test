package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import util.LatticeUtils;
import util.StrUtils;
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
	
	private static void addQuestion(HashMap<String, HashSet<String>> amap,
			String qlabel, String question) {
		if (!amap.containsKey(qlabel)) {
			amap.put(qlabel, new HashSet<String>());
		}
		amap.get(qlabel).add(question);
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
	
	private static String printAnswerTokens(SRLSentence sentence,
			int[] answerFlags) {
		String ret = "";
		for (int i = 0; i < answerFlags.length; i++) {
			if (answerFlags[i] > 0) {
				ret += String.format("%s(%d) ",
						sentence.getTokenString(i), answerFlags[i]);
			}
		}
		return ret;
	}
	
	public static void aggregateAnnotations(
			HashMap<Integer, AnnotatedSentence> qaSents,
			HashMap<Integer, AnnotatedSentence> s2Sents,
			SRLCorpus trainCorpus,
			ArrayList<AnnotatedSentence> testSents) {
		
		int numQAs = 0,
			numAggregatedQAs = 0,
			numGoodFilters = 0,
			numBadFilters = 0;
		
		SRLAnnotationValidator validator = new SRLAnnotationValidator();
		//validator.ignoreLabels = true;
		
		for (int sentId : qaSents.keySet()) {
			AnnotatedSentence qaSent = qaSents.get(sentId);
			SRLSentence sent = (SRLSentence) qaSent.sentence;
			int s1Props = qaSent.qaLists.size(),
				s2Props = 0;
			
			String[][] gold = validator.getGoldSRL(sent);
			
			int[][] covered = new int[gold.length][];
			for (int i = 0; i < gold.length; i++) {
				covered[i] = new int[gold[i].length];
			}
			LatticeUtils.fill(covered, 0);
		
			int sentLength = sent.length;
			
			for (Proposition prop : sent.propositions) {
				int propHead = prop.propID;
				if (!qaSent.qaLists.containsKey(propHead)) {
					continue;
				}
				//for (int propHead : qaSent.qaLists.keySet()) {
				// QLabel to Questions
				HashMap<String, HashSet<String>>
					s1QMap = new HashMap<String, HashSet<String>>(),
					s2QMap = new HashMap<String, HashSet<String>>();
				// QLabel to Answer Flags
				HashMap<String, int[]>
					s1AMap = new HashMap<String, int[]>(),
					s2AMap = new HashMap<String, int[]>();			 

				for (QAPair qa : qaSent.qaLists.get(propHead)) {
					addQuestion(s1QMap, qa.getQuestionLabel(),
							qa.getQuestionString());
					addAnswer(s1AMap, qa.getQuestionLabel(), qa.answerFlags);
				}
				
				if (s2Sents.containsKey(sentId)) {
					AnnotatedSentence s2Sent = s2Sents.get(sentId);
					s2Props = s2Sent.qaLists.size();
					if (s2Sent.qaLists.containsKey(propHead)) {
						for (QAPair qa : s2Sent.qaLists.get(propHead)) {
							addQuestion(s2QMap, qa.getQuestionLabel(),
									qa.getQuestionString());
							addAnswer(s2AMap, qa.getQuestionLabel(),
									qa.answerFlags);
						}
						
						// Print sentence info.
						System.out.println();
						System.out.println(sent.sentenceID + "\t" + sent.getTokensString());
						System.out.println(sent.getTokenString(propHead));
						for (int i = 0; i < prop.argIDs.size(); i++) {
							int argType = prop.argTypes.get(i),
								argId = prop.argIDs.get(i);
							System.out.println(
									" \t" + trainCorpus.argModDict.getString(argType) + "\t" +
									" \t" + sent.getTokenString(argId) + "\t");
						}
					}
				}
				
				ArrayList<QAPair> newList = new ArrayList<QAPair>();
				
				// Compute Stage1-Stage2 Agreement
				for (String qlabel : s1AMap.keySet()) {
					boolean disagree = false,
							matched = false;
					int[] flags1 = s1AMap.get(qlabel);
					QAPair newQA = new QAPair(qaSent.sentence, propHead,
							qlabel, "" /* answer */, null /* cfResult */);
					LatticeUtils.copy(newQA.answerFlags, flags1);
					
					if (s2AMap.containsKey(qlabel)) {
						int[] flags2 = s2AMap.get(qlabel);
						int overlap = 0, all = 0, minS1 = 0;
						for (int i = 0; i < sentLength; i++) {
							overlap += Math.min(flags1[i], flags2[i]);
							all += Math.max(flags1[i], flags2[i]);
							minS1 = Math.min(minS1, flags1[i]);
						}
						//System.out.println(overlap + ", " + all);
						double overlapRatio = 1.0 * overlap / all;
						if (overlap == 0) {
							disagree = true;
						}
					}
					
					if (!disagree) {
						if (s2AMap.containsKey(qlabel)) {
							newQA.addAnswer(s2AMap.get(qlabel));
						}
						newList.add(newQA);
					}

					for (int i = 0; i < sentLength; i++) {
						if (!gold[propHead + 1][i + 1].isEmpty() &&
							validator.matchedGold(i, newQA, sent)) {
							covered[propHead + 1][i + 1] = 1;
							matched = true;
						}
					}
					
					if (s2AMap.containsKey(qlabel)) {
						// Print debug output.
						System.out.println(
							qlabel + "\t" +
							StrUtils.join(",", s1QMap.get(qlabel).toArray()) + "\t" +
							StrUtils.join(",", s2QMap.get(qlabel).toArray()) + "\t" +
							printAnswerTokens(sent, s1AMap.get(qlabel)) + "\t" + 
							printAnswerTokens(sent, s2AMap.get(qlabel)) + "\t" + 
							(disagree ? "NA" : " ") + "\t" +
							(matched ? " " : "NG") + "\t");
						
						if (disagree && !matched) {
							numGoodFilters ++;
						} else if(disagree && matched) {
							numBadFilters ++;
						}
					}
				}
				qaSent.qaLists.put(propHead, newList);
				if (s2Props == s1Props) {
					testSents.add(qaSent);
					numQAs += s1AMap.size();
					numAggregatedQAs += newList.size();
				}
			}
		}
		System.out.println(
				   "Num QAs before filtering:\t" + numQAs +
		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
		System.out.println(
				 	"Number of good filters:\t" + numGoodFilters + "\n" +
					"Number of bad filters:\t" + numBadFilters);
		System.out.println("Number of test sentences\t" + testSents.size());
		
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
		
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
		
		aggregateAnnotations(qaSents, s2Sents, trainCorpus, testSents);
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		//tester.ignoreLabels = true;
		tester.computeSRLAccuracy(testSents, trainCorpus);
	}
}
