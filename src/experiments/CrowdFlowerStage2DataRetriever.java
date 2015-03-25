package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
				for (String answer : s2Result.answers) {
					qa.addAnswer(answer);
				}
				currSent.addQAPair(propHead, qa);
			}
		}
	}
	
	public static void aggregateAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerQAResult> stage1Results,
			ArrayList<CrowdFlowerStage2Result> stage2Results) {
		int numQAs = 0,
			numAggregatedQAs = 0;
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				ArrayList<QAPair> qaList =
						annotSent.qaLists.get(propHead);
				HashMap<String, Integer> qmap = new HashMap<String, Integer>();
				
				for (QAPair qa : qaList) {
					String qstr = qa.getQuestionLabel();
					int k = (qmap.containsKey(qstr) ? qmap.get(qstr) : 0);
					qmap.put(qstr, k + 1);
				}
				ArrayList<QAPair> newList =
						new ArrayList<QAPair>();
				
				for (String qlabel : qmap.keySet()) {
					// Remove unagreed or bad question labels.
					if (qmap.get(qlabel) <= 1 || qlabel.contains("???")) {
						continue;
					}
					QAPair newQA = new QAPair(
							annotSent.sentence,
							propHead,
							qlabel,
							"" /* answer */,
							null /* cfAnnotationResult */);
					for (QAPair qa : qaList) {
						if (qa.getQuestionLabel().equals(qlabel)) {
							newQA.addAnswer(qa.answerFlags);
						}
					}
					newList.add(newQA);
				}
				numQAs += annotSent.qaLists.get(propHead).size();
				numAggregatedQAs += newList.size();
				annotSent.qaLists.put(propHead, newList);
			}
		}
		System.out.println("Num QAs before filtering:\t" + numQAs +
		 		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
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
			readAnnotationResult(qaFilePath, qaResults,
					AnnotationStage.QuestionAnswerStage);
			readAnnotationResult(s2FilePath, s2Results,
					AnnotationStage.AnswerStage);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		alignAnnotations(qaSents, qaResults, trainCorpus);
		alignAnnotations(s2Sents, qaResults, trainCorpus);
		
	}
}
