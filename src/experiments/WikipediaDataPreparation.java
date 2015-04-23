package experiments;

import io.XSSFOutputHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import data.Proposition;
import data.Sentence;
import data.VerbInflectionDictionary;
import data.WikipediaCorpus;

public class WikipediaDataPreparation {
	
	private static WikipediaCorpus wikiCorpus = null;
	private static VerbInflectionDictionary inflDict = null;
	
	private static ArrayList<Sentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	
	private static boolean lookupInflections(String propHead) {
		ArrayList<Integer> inflIds = inflDict.inflMap.get(propHead);
		if (inflIds == null) {
			System.out.println("Unidentified verb:\t" + propHead);
			return false;
		}
		return true;
	}
	
	private static void extractPropositions() {
		PosTaggerVerbIdentifier verbId = new PosTaggerVerbIdentifier(wikiCorpus);
		
		for (int i = 0; i < wikiCorpus.sentences.size(); i++) {
			Sentence sentence = wikiCorpus.sentences.get(i);
			ArrayList<Integer> propIds = verbId.extractContentVerbs(sentence);
			boolean hasUnknownVerb = false;
			for (int propId : propIds) {
				String verb = sentence.getTokenString(propId).toLowerCase();
				if (verb.contains("-")) {
					verb = verb.substring(verb.indexOf('-') + 1);
				}
				if (!lookupInflections(verb)) {
					hasUnknownVerb = true;
					break;
				}
			}
			if (hasUnknownVerb || propIds.isEmpty()) {
				continue;
			}
			ArrayList<Proposition> props = new ArrayList<Proposition>();
			for (int propId : propIds) {
				props.add(new Proposition(new int[] {propId, propId + 1}));
			}
			sentences.add(sentence);
			propositions.add(props);
		}
	}
	
	public static void main(String[] args) {
		String inputPath = args[0];
			   
		wikiCorpus = new WikipediaCorpus("wikipedia-all");
		sentences = new ArrayList<Sentence>();
		propositions = new ArrayList<ArrayList<Proposition>>();
		
		try {
			wikiCorpus.maxNumSentences = 3000;
			wikiCorpus.sampleRate = 0.0001;
			wikiCorpus.sampleFromWikipedia(inputPath);
			inflDict = new VerbInflectionDictionary(wikiCorpus);
			inflDict.loadDictionaryFromFile("wiktionary/en_verb_inflections.txt");
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Start tagging " + wikiCorpus.sentences.size() + " sentences ...");
		extractPropositions();
		
		System.out.println("Extracted " + sentences.size() + " sentences.");

		ArrayList<Sentence> currSents = new ArrayList<Sentence>();
		ArrayList<ArrayList<Proposition>> currProps = new ArrayList<ArrayList<Proposition>>();
		XSSFOutputHelper.maxNumSheetsPerFile = 5;
		XSSFOutputHelper.maxNumSentsPerSheet = 12;
		int numSentsPerFile = XSSFOutputHelper.maxNumSentsPerSheet *
						      XSSFOutputHelper.maxNumSheetsPerFile;
		int numFiles = sentences.size()  / numSentsPerFile - 1;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
					"odesk_wiki/odesk_wiki1.meta_new.txt"))); 
			for (int i = 0; i < numFiles; i++) {
				currSents.clear();
				currProps.clear();
				int startId = i * numSentsPerFile,
					endId = (i + 1) * numSentsPerFile;
				for (int j = startId; j < endId; j++) {
					currSents.add(sentences.get(j));
					ArrayList<Proposition> props = new ArrayList<Proposition>();
					for (Proposition k : propositions.get(j)) {
						props.add(k);
					}
					// Wildcard prop.
					// props.add(new Proposition(new int[] {-1, -1}));
					currProps.add(props);
				}
//				XSSFOutputHelper.outputXlsx(currSents, currProps, inflDict,
//						String.format("odesk_wiki/odesk_wiki1_r%03d.xlsx", i));
				for (Sentence sent : currSents) {
					int docId = wikiCorpus.docIds.get(sent.sentenceID);
					int paraId = wikiCorpus.paragraphIds.get(sent.sentenceID);
					int sentId = wikiCorpus.sentenceIds.get(sent.sentenceID);
					String docTitle = wikiCorpus.wikiInfo.get(docId).title;
					writer.write(String.format("%d\t%d\t%s\t%d\t%d\t%s\n",
							sent.sentenceID, docId, docTitle, paraId, sentId,
							sent.getTokensString()));
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
