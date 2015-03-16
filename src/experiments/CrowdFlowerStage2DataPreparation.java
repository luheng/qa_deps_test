package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import annotation.AuxiliaryVerbIdentifier;
import annotation.CrowdFlowerQAResult;
import annotation.QASlotPrepositions;
import annotation.QuestionEncoder;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.VerbInflectionDictionary;
import util.StringUtils;
import gnu.trove.list.array.TIntArrayList;

public class CrowdFlowerStage2DataPreparation {	
	private static final int randomSeed = 12345;
	
	private static SRLCorpus trainCorpus = null;
	private static ArrayList<CrowdFlowerQAResult> stage1Results = null;
	private static HashMap<Integer, CrowdFlowerQAResult> cfResultsMap = null;
	private static HashMap<Integer, HashMap<String, TIntArrayList>> questionMap;
	
	
	
	private static String outputFileName = "crowdflower/CF_QA_firstround_stage2.csv";

	private static String[] kHeader = {
		"sent_id", "sentence", "orig_sent",
		"prop_id", "prop_start", "prop_end", "proposition",
		"stage1_id", "orig_question", "question", "question_label"};
	
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
	
	private static String getHighlightedQuestion(String question) {
		// Question format: WH, AUX, PH1, TRG, PH2, PP, PH3
		String[] qwords = question.split("\\s+");
		String questionStr = "";
		for (int i = 0; i < 3; i++) {
			questionStr += qwords[i] + " ";
		}
		questionStr += "<mark>" + qwords[3] + "</mark>";
		for (int i = 4; i < 7; i++) {
			questionStr += " " + qwords[i];
		}
		return questionStr;
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
		
		for (int sentId : questionMap.keySet()) {
			SRLSentence sent = (SRLSentence) trainCorpus.sentences.get(sentId);

			for (String question : questionMap.get(sentId).keySet()) {
				int[] cfIds = questionMap.get(sentId).get(question).toArray();
				CrowdFlowerQAResult cfResult = cfResultsMap.get(cfIds[0]);
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
						StringUtils.intArrayToString(" ", cfIds)));
				row.add(getHighlightedQuestion(question));
				row.add(question);
				row.add(QuestionEncoder.encode(question.split("\\s+"), sent));
				csvWriter.printRecord(row);
			}
		}
		csvWriter.close();
	}
	
	private static void aggregateStage1Results() {
		cfResultsMap = new HashMap<Integer, CrowdFlowerQAResult>();
		questionMap = new HashMap<Integer, HashMap<String, TIntArrayList>>();
		int totalNumQuestions = 0;
		for (CrowdFlowerQAResult result : stage1Results) {
			int sentId = result.sentenceId,
				cfId = result.cfUnitId;
			cfResultsMap.put(cfId, result);
			if (!questionMap.containsKey(sentId)) {
				questionMap.put(sentId, new HashMap<String, TIntArrayList>());
			}
			for (String[] question : result.questions) {
				String qstr = StringUtils.join(" ", question);
				if (!questionMap.get(sentId).containsKey(qstr)) {
					questionMap.get(sentId).put(qstr, new TIntArrayList());
				}
				questionMap.get(sentId).get(qstr).add(cfId);
			}
		}
		for (int sentId : questionMap.keySet()) {
			totalNumQuestions += questionMap.get(sentId).size();
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
