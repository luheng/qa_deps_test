package experiments;

import gnu.trove.list.array.TIntArrayList;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import annotation.AnnotationStage;
import annotation.AuxiliaryVerbIdentifier;
import annotation.CrowdFlowerQAResult;
import annotation.CrowdFlowerResult;
import annotation.CrowdFlowerStage2Result;
import annotation.QASlotPrepositions;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.VerbInflectionDictionary;

public class CrowdFlowerQADataPreparation {
	
	private static SRLCorpus trainCorpus = null;
	private static final int maxNumSentences = 200;
	private static final int maxSentenceID = 5000;
	
	private static final int randomSeed = 12345;
	
	private static ArrayList<SRLSentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	
	private static VerbInflectionDictionary inflDict = null;
	private static AuxiliaryVerbIdentifier auxVerbID = null;
	
	private static String[] previousFileNames = new String[] {
			"crowdflower/CF_QA_firstround_s100.csv"
		};
	private static String outputFileName =
			"crowdflower/CF_QA_firstround_r2_s100.csv";

	private static String[] kHeader = {"sent_id", "sentence", "orig_sent",
		"prop_id", "prop_head", "prop_start", "prop_end", "proposition",
		"trg_options", "pp_options"};
	
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
			int propHeadId) {
		String propHead = sent.getTokenString(propHeadId);
		ArrayList<Integer> inflIds =
				inflDict.inflMap.get(propHead.toLowerCase());
		
		if (inflIds == null) {
			//System.out.println("!!! Error:\t" + sent.getTokensString() + "\n" + propHead + " not found");
			System.out.println(propHead);
			//System.out.println(sent.getPostagsString());
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
		// Changes: removed all the ing words.
		HashSet<String> opSet= new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		String[] inflections = inflDict.inflections.get(bestId);
		for (int i = 0; i < inflections.length; i++) {
			if (i != 2) {
				opSet.add(inflections[i]);
			}
		}
		opSet.add("be " + inflections[4]);
		opSet.add("have " + inflections[4]);
		opSet.add("have been " + inflections[4]);
		/*
 		opSet.add("been " + inflections[4]);
		opSet.add("being " + inflections[4]);
		opSet.add("be " + inflections[2]);
		opSet.add("been " + inflections[2]);
		opSet.add("have been " + inflections[2]);
		*/
		for (String op : opSet) {
			options.add(op);
		}
		Collections.sort(options);
		//options.add(0, " ");
		return options;
	}
	
	private static ArrayList<String> getPPOptions(DepSentence sent) {
		HashSet<String> opSet = new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		for (int i = 0; i < sent.length; i++) {
			String tok = sent.getTokenString(i).toLowerCase();
			if (QASlotPrepositions.ppSet.contains(tok)) {
				opSet.add(tok);
				if (i < sent.length - 1) {
					String tok2 = sent.getTokenString(i + 1).toLowerCase();
					if (QASlotPrepositions.ppSet.contains(tok2)) {
						opSet.add(tok + " " + tok2);
						System.out.println(sent.getTokensString());
						System.out.println(tok + " " + tok2);
					}
				}
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
	// "sent_id", "sentence", "orig_sent", "prop_id", "prop_head", "prop_start", "prop_end", "proposition", "trg_options"
	private static void outputUnits() throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		CSVPrinter csvWriter = new CSVPrinter(fileWriter, CSVFormat.EXCEL
				.withRecordSeparator("\n"));
		csvWriter.printRecord((Object[]) kHeader);
		for (int i = 0; i < maxNumSentences; i++) {
			SRLSentence sent = sentences.get(i);
			ArrayList<Proposition> props = propositions.get(i);
			if (props.size() == 0) {
				continue;
			}
			ArrayList<String> ppOptions = getPPOptions(sent);
			for (int j = 0; j < props.size(); j++) {
				Proposition prop = props.get(j);
				ArrayList<String> trgOptions = getTrgOptions(sent,
						prop.span[1] - 1);
				if (trgOptions == null) {
					continue;
				}
				ArrayList<String> row = new ArrayList<String>();
				row.add(String.valueOf(sent.sentenceID));
				row.add(getPartiallyHighlightedSentence(sent, prop));
				row.add(sent.getTokensString());
				row.add(String.valueOf(j));
				row.add(String.valueOf(prop.span[1] - 1));
				row.add(String.valueOf(prop.span[0]));
				row.add(String.valueOf(prop.span[1]));
				row.add(sent.getTokenString(prop.span));
				row.add(getCMLOptions(trgOptions));
				row.add(getCMLOptions(ppOptions));
				csvWriter.printRecord(row);
			}
		}
		csvWriter.close();
	}
	
	private static boolean isQuestion(DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			String word = sentence.getTokenString(i);
			if (word.equals("?")) {
				return true;
			}
		}
		return false;
	}
	
	private static int[] getNonQuestionSentenceIds() {
		TIntArrayList ids = new TIntArrayList();
		for (DepSentence sentence : trainCorpus.sentences) {
			if (sentence.sentenceID > maxSentenceID) {
				break;
			}
			if (!isQuestion(sentence) && sentence.length >= 10) {
				ids.add(sentence.sentenceID);
			}
		}
		return ids.toArray();
	}
	
	private static HashSet<Integer> getAnnotatedSentenceIds() {
		HashSet<Integer> annotatedIds = new HashSet<Integer>();
		FileReader fileReader = null;
		Iterable<CSVRecord> records = null;
		for (String filePath : previousFileNames) {
			try {
				fileReader = new FileReader(filePath);
				records = CSVFormat.DEFAULT.withHeader().parse(fileReader);
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (CSVRecord record : records) {
				int sentId = Integer.parseInt(record.get("sent_id"));
				annotatedIds.add(sentId);
			}
		}
		return annotatedIds;
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		inflDict = new VerbInflectionDictionary(trainCorpus);
		try {
			inflDict.loadDictionaryFromFile("wiktionary/en_verb_inflections.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		auxVerbID = new AuxiliaryVerbIdentifier(trainCorpus);
		sentences = new ArrayList<SRLSentence>();
		propositions = new ArrayList<ArrayList<Proposition>>();
		
		int[] nonQuestionIds = getNonQuestionSentenceIds();		
		System.out.println("Number of non-question sentences:\t" +
						   nonQuestionIds.length);
		
		// Exclude sentences that are already annotated ...
		HashSet<Integer> annotatedIds = getAnnotatedSentenceIds();
		
		// Throw away sentences that contains verbs we don't identify ...
		for (int id : nonQuestionIds) {
			if (annotatedIds.contains(id)) {
				continue;
			}
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(id);
			boolean containsUnidentifiedVerb = false;
			int numTargets = 0;
			for (int j = 0; j < sentence.length; j++) {
				if (sentence.getPostagString(j).equals("VERB") &&
					!auxVerbID.ignoreVerbForSRL(sentence, j)) {
					if (getTrgOptions(sentence, j) == null) {
						containsUnidentifiedVerb = true;
					}
					numTargets ++;
					break;
				}
			}
			if (!containsUnidentifiedVerb && numTargets > 0) {
				sentences.add(sentence);
			}
		}
		System.out.println("Sentences left:\t" + sentences.size());
		
		Collections.shuffle(sentences, new Random(randomSeed));
	
		for (int i = 0; i < sentences.size(); i++) {
			SRLSentence sentence = sentences.get(i);
			ArrayList<Proposition> props = new ArrayList<Proposition>();
			for (int j = 0; j < sentence.length; j++) {
				if (sentence.getPostagString(j).equals("VERB") &&
					!auxVerbID.ignoreVerbForSRL(sentence, j)) {					
					Proposition prop = new Proposition();
					prop.sentence = sentence;
					prop.setPropositionSpan(j, j + 1);
					props.add(prop);
				}
			}
			propositions.add(props);
		}
		
		try {
			outputUnits();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}