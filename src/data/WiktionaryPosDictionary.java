package data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import experiments.ExperimentUtils;

public class WiktionaryPosDictionary {	
	DepCorpus corpus;
	public HashMap<String, int[]> posMap;
	public CountDictionary posDict;
	
	public WiktionaryPosDictionary(DepCorpus corpus) {
		this.corpus = corpus;
		posMap = new HashMap<String, int[]>();
		posDict = new CountDictionary();
	}
	
	public void loadDictionaryFromFile(String filePath) throws IOException {
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				String[] info = line.split("\t");
				String word = info[0];
				String[] tags = info[1].split(" ");
				int[] tagIds = new int[tags.length];
				for (int i = 0; i < tags.length; i++) {
					tagIds[i] = posDict.addString(tags[i].trim());
				}
				posMap.put(word, tagIds);
			}
		}
		reader.close();
		System.out.println(String.format("Read %d entries from %s.",
				posMap.size(), filePath));
	}
	
	public static void main(String[] args) {
		SRLCorpus corpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
		WiktionaryPosDictionary wikposDict = new WiktionaryPosDictionary(corpus);
		try {
			wikposDict.loadDictionaryFromFile("wiktionary/en_postags_withverb.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
