package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	private static SRLCorpus trainCorpus = null;
	private static ArrayList<CrowdFlowerQAResult> stage1Results = null;
	private static HashMap<String, CrowdFlowerQAResult> cfResultsMap = null;
	private static HashMap<Integer,
		HashMap<Integer, HashMap<String, TIntArrayList>>> questionMap = null;
	
	private static int maxNumRows = 100;
	
	private static String outputFileName = "crowdflower/CF_QA_firstround_stage2.csv";

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
		questionStr += "<mark>" + qwords[3] + "</mark>";
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
		FileWriter fileWriter = new FileWriter(outputFileName);
		CSVPrinter csvWriter = new CSVPrinter(fileWriter, CSVFormat.EXCEL
				.withRecordSeparator("\n"));
		csvWriter.printRecord((Object[]) kHeader);
		int rowCounter = 0;
		for (int sentId : questionMap.keySet()) {
			SRLSentence sent = (SRLSentence) trainCorpus.sentences.get(sentId);
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
					row.add(String.valueOf(
							StringUtils.intArrayToString(" ", workerIds)));
					row.add(getHighlightedQuestion(qwords));
					row.add(qstr);
					row.add(QuestionEncoder.encode(qwords, sent));
					csvWriter.printRecord(row);
					
					rowCounter ++;
					if (rowCounter >= maxNumRows) {
						csvWriter.close();
						System.out.println(String.format("Output %d rows to file %s.",
								rowCounter, outputFileName));
						return;
					}
				}
			}
		}
		csvWriter.close();
		System.out.println(String.format("Output %d rows to file %s.",
				rowCounter, outputFileName));
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
		trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		
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
