package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import data.Proposition;
import data.SRLSentence;

public class CrowdFlowerQADataPreparation {
	
	private static ArrayList<SRLSentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	private static String outputFileName = "crowdflower/CF_QA_trial_s20.tsv";
	
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
				String row = sent.sentenceID + "\t";
				row += getPartiallyHighlightedSentence(sent, prop) + "\t";
				row += sent.getTokensString() + "\t";
				row += j + "\t";
				row += sent.getTokenString(prop.span) + "\n";
				bufferedWriter.write(row);
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
		
		// Step 2: write to CSV data. Each row contains a sentence and an
		// identified proposition.
		try {
			outputUnits();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
