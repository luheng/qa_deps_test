package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import util.StringUtils;
import data.Proposition;
import data.SRLSentence;
import data.VerbInflectionDictionary;

public class CrowdFlowerQADataPreparation {
	
	private static ArrayList<SRLSentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	
	private static VerbInflectionDictionary inflDict = null;
	
	private static String outputFileName = "crowdflower/CF_QA_trial_s20.test.tsv";
	
	
	private static String kHeaderRow =
			"sent_id\tsentence\torig_sent\tprop_id\tproposition\n";
	
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
	
	private static ArrayList<String> getPropositionChoices(Proposition prop) {
		int propStart = prop.span[0], propEnd = prop.span[1];
		String propHead = prop.sentence.getTokenString(propEnd - 1);
		ArrayList<Integer> inflIds;
		try {
			inflIds = inflDict.inflMap.get(propHead);
		} catch (NullPointerException e) {
			System.out.println("!!! Error:\t" + propHead + " not found");
			return null;
		}
		String propStr = prop.sentence.getTokenString(prop.span);
		
		int bestId = -1, bestCount = -1;
		for (int i = 0; i < inflIds.size(); i++) {
			int count = inflDict.inflCount[inflIds.get(i)];
			if (count > bestCount) {
				bestId = inflIds.get(i);
				bestCount = count;
			}
		}
		
		//System.out.println(StringUtils.join(" ", inflDict.inflections.get(bestId)));
		// Generate list for dropdown.
		ArrayList<String> choices = new ArrayList<String>();
		String[] inflections = inflDict.inflections.get(bestId);
		for (String infl : inflections) {
			choices.add(infl);
		}
		choices.add("be " + inflections[4]);
		choices.add("been " + inflections[4]);
		choices.add("being " + inflections[4]);
		choices.add("have " + inflections[4]);
		choices.add("have been " + inflections[4]);
		choices.add("be " + inflections[2]);
		choices.add("been " + inflections[2]);
		choices.add("have been " + inflections[2]);
		return choices;
	}
	
	private static void outputUnits() throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
		// Write file header:
		//	sent_id: sentence id
		//  sentence: sentence with highlight mark
		//  orig_sent: original sentence
		//  prop_id: proposition id, starting from 0 for each sentence
		//  proposition: the identified proposition.
		
		bufferedWriter.write(kHeaderRow);
		
		int numSentences = sentences.size();
		for (int i = 0; i < numSentences; i++) {
			SRLSentence sent = sentences.get(i);
			ArrayList<Proposition> props = propositions.get(i);
			if (props.size() == 0) {
				continue;
			}
			for (int j = 0; j < props.size(); j++) {
				Proposition prop = props.get(j);
				ArrayList<String> choices = getPropositionChoices(prop);
				
				String row = sent.sentenceID + "\t";
				row += getPartiallyHighlightedSentence(sent, prop) + "\t";
				row += sent.getTokensString() + "\t";
				row += j + "\t";
				row += sent.getTokenString(prop.span) + "\n";
				bufferedWriter.write(row);
				
				System.out.println(row);
				for (String ch : choices) {
					System.out.println(ch);
				}
				System.out.println();
			}
		}
		bufferedWriter.close();
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
