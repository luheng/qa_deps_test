package data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import util.StringUtils;
import experiments.ExperimentUtils;

public class VerbInflectionDictionary {
	
	//private static String verbInflectionsFilePath =
	//		"wiktionary/en_verb_inflections.txt";
	
	DepCorpus corpus;
	public ArrayList<String[]> inflections;
	public int[] inflCount;
	public HashMap<String, ArrayList<Integer>> inflMap;
	
	public VerbInflectionDictionary(DepCorpus corpus) {
		this.corpus = corpus;
		inflections = new ArrayList<String[]>();
		inflMap = new HashMap<String, ArrayList<Integer>>();
	}
	
	public void loadDictionaryFromFile(String filePath) throws IOException {
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				String[] strs = line.split("\t");
				String[] infl = new String[5];
				boolean inCorpus = false;
				for (int i = 0; i < 5; i++) {
					infl[i] = strs[i];
					if (!inCorpus && corpus.wordDict.contains(infl[i])) {
						inCorpus = true;
					}
				}
				int inflId = inflections.size();
				inflections.add(infl);
				for (int i = 0; i < infl.length; i++) {
					String v = infl[i];
					if (v.equals("_") || v.equals("-")) {
						continue;
					}
					if (!inflMap.containsKey(v)) {
						inflMap.put(v, new ArrayList<Integer>()); 
					}
					ArrayList<Integer> inflIds = inflMap.get(v);
					if (!inflIds.contains(inflId)) {
						inflIds.add(inflId);
					}
				}
			}
		}
		reader.close();
		
		countInflections();
	}
	
	public int getBestInflectionId(String verb) {
		ArrayList<Integer> inflIds = inflMap.get(verb);
		if (inflIds == null) {
			//System.out.println("!!! Error:\t" + sent.getTokensString() + "\n" + propHead + " not found");
			System.out.println(verb);
			//System.out.println(sent.getPostagsString());
			return -1;
		}
		int bestId = -1, bestCount = -1;
		for (int i = 0; i < inflIds.size(); i++) {
			int count = inflCount[inflIds.get(i)];
			if (count > bestCount) {
				bestId = inflIds.get(i);
				bestCount = count;
			}
		}
		return bestId;
	}
	
	public String getBestBaseVerb(String verb) {
		int bestId = getBestInflectionId(verb);
		return bestId < 0 ? verb : inflections.get(bestId)[0];
	}
	
	private void countInflections() {
		inflCount = new int[inflections.size()];
		Arrays.fill(inflCount, 0);
		for (DepSentence sent : corpus.sentences) {
			for (int i = 0; i < sent.length; i++) {
				String w = sent.getTokenString(i);
				if (inflMap.containsKey(w)) {
					for (int inflId : inflMap.get(w)) {
						inflCount[inflId] ++;
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus corpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		VerbInflectionDictionary inflDict = new VerbInflectionDictionary(corpus);
		try {
			inflDict.loadDictionaryFromFile("wiktionary/en_verb_inflections.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		for (String verb : inflDict.inflMap.keySet()) {
			ArrayList<Integer> inflIds = inflDict.inflMap.get(verb);
			if (inflIds.size() > 1) {
				for (int id : inflIds) {
					System.out.print(inflDict.inflCount[id] + "\t");
					System.out.println(StringUtils.join("\t",
							inflDict.inflections.get(id)));
				}
				System.out.println();
			}
		}
		*/
	}
}
