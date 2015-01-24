package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import annotation.QASlotPrepositions;
import data.DepSentence;
import data.Proposition;
import data.SRLSentence;
import data.VerbInflectionDictionary;

public class CrowdFlowerQADataPreparation {
	
	private static ArrayList<SRLSentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	
	private static VerbInflectionDictionary inflDict = null;
	
	private static String outputFileName = "crowdflower/CF_QA_trial_s20.csv";
	
	
	//private static String kHeaderRow =
	//		"sent_id\tsentence\torig_sent\tprop_id\tproposition\ttrg_options";
	private static String[] kHeader = {"sent_id", "sentence", "orig_sent",
		"prop_id", "proposition", "trg_options", "pp_options"};
	
	private static String getPartiallyHighlightedSentence(SRLSentence sentence,
			Proposition prop) {
		String sentStr = "";
		int s1 = prop.span[0], s2 = prop.span[1];
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
	
	private static ArrayList<String> getTrgOptions(DepSentence sent,
			Proposition prop) {
		String propHead = prop.sentence.getTokenString(prop.span[1] - 1);
		ArrayList<Integer> inflIds;
		try {
			inflIds = inflDict.inflMap.get(propHead);
		} catch (NullPointerException e) {
			System.out.println("!!! Error:\t" + propHead + " not found");
			return null;
		}
		
		int bestId = -1, bestCount = -1;
		for (int i = 0; i < inflIds.size(); i++) {
			int count = inflDict.inflCount[inflIds.get(i)];
			if (count > bestCount) {
				bestId = inflIds.get(i);
				bestCount = count;
			}
		}
		// Generate list for dropdown.
		HashSet<String> opSet= new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		String[] inflections = inflDict.inflections.get(bestId);
		for (String infl : inflections) {
			opSet.add(infl);
		}
		opSet.add("be " + inflections[4]);
		opSet.add("been " + inflections[4]);
		opSet.add("being " + inflections[4]);
		opSet.add("have " + inflections[4]);
		opSet.add("have been " + inflections[4]);
		opSet.add("be " + inflections[2]);
		opSet.add("been " + inflections[2]);
		opSet.add("have been " + inflections[2]);
		for (String op : opSet) {
			options.add(op);
		}
		Collections.sort(options);
		options.add(0, " ");
		return options;
	}
	
	private static ArrayList<String> getPPOptions(DepSentence sent) {
		HashSet<String> opSet = new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		for (String pp : QASlotPrepositions.values) {
			if (sent.containsToken(pp)) {
				opSet.add(pp);
			}
		}
		for (String pp : QASlotPrepositions.mostFrequentPPs) {
			opSet.add(pp);
		}
		for (String op : opSet) {
			options.add(op);
		}
		Collections.sort(options);
		options.add(0, " ");
		return options;
	}
	
	private static String getCMLOptions(ArrayList<String> options) {
		String result = "";
		for (String option : options) {
			if (!result.isEmpty()) {
				result += "#";
			}
			result += option;
		}
		return result;
	}
	
	// Output the following fields:
	// "sent_id", "sentence", "orig_sent", "prop_id", "proposition", "trg_options"
	private static void outputUnits() throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		CSVPrinter csvWriter = new CSVPrinter(fileWriter, CSVFormat.EXCEL
				.withRecordSeparator("\n"));
		csvWriter.printRecord((Object[]) kHeader);
		for (int i = 0; i < sentences.size(); i++) {
			SRLSentence sent = sentences.get(i);
			ArrayList<Proposition> props = propositions.get(i);
			if (props.size() == 0) {
				continue;
			}
			ArrayList<String> ppOptions = getPPOptions(sent);
			for (int j = 0; j < props.size(); j++) {
				Proposition prop = props.get(j);
				ArrayList<String> trgOptions = getTrgOptions(sent, prop);
				ArrayList<String> row = new ArrayList<String>();
				row.add(String.valueOf(sent.sentenceID));
				row.add(getPartiallyHighlightedSentence(sent, prop));
				row.add(sent.getTokensString());
				row.add(String.valueOf(j));
				row.add(sent.getTokenString(prop.span));
				row.add(getCMLOptions(trgOptions));
				row.add(getCMLOptions(ppOptions));
				csvWriter.printRecord(row);
			}
		}
		csvWriter.close();
	}
	
	public static void main(String[] args) {
		// Step 1: get proposition data..
		sentences = new ArrayList<SRLSentence>();
		propositions = new ArrayList<ArrayList<Proposition>>();
		CrowdFlowerPropIdDataRetriever.readIdentifiedPropositions(sentences,
				propositions);
		
		// Step 2: Read inflection dictionary
		inflDict = new VerbInflectionDictionary(sentences.get(0).corpus);
		try {
			inflDict.loadDictionaryFromFile("wiktionary/en_verb_inflections.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Step 3: write to CSV data. Each row contains a sentence and an
		// identified proposition.
		try {
			outputUnits();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
