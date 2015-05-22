package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import annotation.CrowdFlowerQAResult;
import annotation.QuestionEncoder;
import data.SRLCorpus;
import data.SRLSentence;
import util.StringUtils;
import gnu.trove.list.array.TIntArrayList;

public class CrowdFlowerStage2DataPreparation {	
	private static final int randomSeed = 12345;
	private static int numTestSentences = 0;
	private static int maxNumRows = Integer.MAX_VALUE;
	
	private static SRLCorpus trainCorpus = null;
	private static ArrayList<CrowdFlowerQAResult> stage1Results = null;
	// Maps sent_id + prop_id + worker_id to cf result.
	private static HashMap<String, CrowdFlowerQAResult> cfResultsMap = null;
	// Maps sent_id -> prp_id -> question string to list of worker ids/
	private static HashMap<Integer,
		HashMap<Integer, HashMap<String, TIntArrayList>>> questionMap = null;
		
	private static String validationFileName = "crowdflower/CF_QA_firstround_stage2_tmp.csv",
						  collectionFileName = "crowdflower/CF_QA_firstround_stage2_tmp.csv";

	private static String[] kHeader = {
		"sent_id", "sentence", "orig_sent",
		"prop_id", "prop_start", "prop_end", "proposition",
		"stage1_id", "question", "orig_question", "question_label"};
	
	private static String getHighlightedSentence(SRLSentence sentence, int s1,
			int s2) {
		String sentStr = "";
		for (int i = 0; i < sentence.length; i++) {
			if (i > 0) {
				sentStr += " ";
			}
			if (i == s1) {
				sentStr += "<mark>";
			}
			sentStr += sentence.getTokenString(i);
			if (i == s2 - 1) {
				sentStr += "</mark>";
			}
		}
		return sentStr;
	}
	
	private static String getHighlightedQuestion(String[] qwords) {
		// Question format: WH, AUX, PH1, TRG, PH2, PP, PH3
		String questionStr = "";
		for (int i = 0; i < 3; i++) {
			if (!qwords[i].isEmpty()) {
				questionStr += qwords[i] + " ";
			}
		}
		String[] infl = qwords[3].split("\\s+");
		for (int i = 0; i < infl.length - 1; i++) {
			questionStr += infl[i] + " ";
		}
		questionStr += "<mark>" + infl[infl.length - 1] + "</mark>";
		for (int i = 4; i < 7; i++) {
			if (!qwords[i].isEmpty()) {
				questionStr += " " + qwords[i];
			}
		}
		questionStr = questionStr.trim() + "?";
		return questionStr;
	}
	
	private static String[] getQuestionWords(String qstr,
			CrowdFlowerQAResult cfResult) {
		for (String[] question : cfResult.questions) {
			if (StringUtils.join(" ", question).trim().equals(qstr)) {
				return question;
			}
		}
		return null;
	}
	// Output the following fields:
	//   "sent_id", "sentence", "orig_sent",
	//   "prop_id", "prop_start", "prop_end", "proposition",
	//   "stage1_id", "question", "orig_question", "question_label"
	private static void outputUnits() throws IOException {
		// Shuffle sentence ids
		ArrayList<Integer> sentIds = new ArrayList<Integer>();
		for (int sentId : questionMap.keySet()) {
			sentIds.add(sentId);
		}
		Collections.shuffle(sentIds, new Random(randomSeed));
		System.out.println("Validating sentence IDs:");
		for (int i = 0; i < numTestSentences; i++) {
		//	System.out.println(sentIds.get(i) + "\t");
			SRLSentence sent = (SRLSentence) trainCorpus.sentences.get(sentIds.get(i));
			System.out.println(sent.toString());
		}
		System.out.println();
		
		// Prepare to write to validating file.
		CSVPrinter csvWriter = new CSVPrinter(
				new FileWriter(validationFileName),
				CSVFormat.EXCEL.withRecordSeparator("\n"));
		csvWriter.printRecord((Object[]) kHeader);
		
		int rowCounter = 0;
		for (int i = 0; i < sentIds.size(); i++) {
			int sentId = sentIds.get(i);
			SRLSentence sent = (SRLSentence) trainCorpus.sentences.get(sentId);
			
			if (i == numTestSentences) {
				// Switch to collection file.
				csvWriter.close();
				System.out.println(String.format("Output %d rows to file %s.",
						rowCounter, validationFileName));
				csvWriter = new CSVPrinter(
						new FileWriter(collectionFileName),
						CSVFormat.EXCEL.withRecordSeparator("\n"));
				csvWriter.printRecord((Object[]) kHeader);
				rowCounter = 0;
			}
			
			for (int propHead : questionMap.get(sentId).keySet()) {
				for (String qstr : questionMap.get(sentId).get(propHead).keySet()) {
					int[] workerIds = questionMap.get(sentId).get(propHead).get(qstr).toArray();
					String cfKey = String.format("%d_%d_%d", sentId, propHead, workerIds[0]);
					CrowdFlowerQAResult cfResult = cfResultsMap.get(cfKey);
				
					String[] qwords = getQuestionWords(qstr, cfResult);
					if (qwords == null) {
						System.out.println(qstr);
						System.out.println(cfResult.toString());
						continue;
					}
				
					int[] propSpan = new int[2];
					propSpan[0] = cfResult.propStart;
					propSpan[1] = cfResult.propEnd;
				
					ArrayList<String> row = new ArrayList<String>();
					// Sentence Info
					row.add(String.valueOf(sentId));
					row.add(getHighlightedSentence(sent, propSpan[0], propSpan[1]));
					row.add(sent.getTokensString());
					// Proposition Info
					row.add(String.valueOf(cfResult.propositionId));
					row.add(String.valueOf(propSpan[0]));
					row.add(String.valueOf(propSpan[1]));
					row.add(sent.getTokenString(propSpan));
					// Stage 1 Annotation Info
					row.add(StringUtils.intArrayToString(" ", workerIds));
					row.add(getHighlightedQuestion(qwords));
					row.add(qstr);
					row.add(QuestionEncoder.getLabels(qwords)[0]);
					csvWriter.printRecord(row);
					
					rowCounter ++;
					if (rowCounter >= maxNumRows) {
						csvWriter.close();
						System.out.println(
								String.format("Output %d rows to file %s.",
										rowCounter, collectionFileName));
						return;
					}
				}
			}
		}
		csvWriter.close();
		System.out.println(String.format("Output %d rows to file %s.",
				rowCounter, collectionFileName));
	}
	
	private static void aggregateStage1Results() {
		cfResultsMap = new HashMap<String, CrowdFlowerQAResult>();
		questionMap = new HashMap<Integer,
				HashMap<Integer, HashMap<String, TIntArrayList>>>();
		int totalNumQuestions = 0;
		for (CrowdFlowerQAResult result : stage1Results) {
			int sentId = result.sentenceId,
				propHead = result.propEnd - 1,
				workerId = result.cfWorkerId;
			String cfKey = String.format("%d_%d_%d", sentId, propHead, workerId);
			cfResultsMap.put(cfKey, result);
			if (!questionMap.containsKey(sentId)) {
				questionMap.put(sentId,
						new HashMap<Integer, HashMap<String, TIntArrayList>>());
			}
			if (!questionMap.get(sentId).containsKey(propHead)) {
				questionMap.get(sentId).put(propHead,
						new HashMap<String, TIntArrayList>());
			}
			for (String[] question : result.questions) {
				String qstr = StringUtils.join(" ", question).trim();
				if (!questionMap.get(sentId).containsKey(qstr)) {
					questionMap.get(sentId).get(propHead).put(qstr,
							new TIntArrayList());
				}
				questionMap.get(sentId).get(propHead).get(qstr).add(workerId);
			}
		}
		for (int sentId : questionMap.keySet()) {
			for (int propHead : questionMap.get(sentId).keySet()) {
				totalNumQuestions += questionMap.get(sentId).get(propHead).size();
			}
		}
		System.out.println("Total number of questions:\t" + totalNumQuestions);
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
		
		stage1Results = new ArrayList<CrowdFlowerQAResult>();
		try {
			CrowdFlowerQADataRetriever.readAnnotationResult(stage1Results);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		aggregateStage1Results();
	
		// TODO: shuffle sentences
		
		try {
			outputUnits();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
