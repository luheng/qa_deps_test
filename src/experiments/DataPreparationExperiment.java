package experiments;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import util.RandomSampler;
import data.DepCorpus;
import data.DepSentence;

public class DataPreparationExperiment {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static final int maxSentenceID = 10000,
						    numToLabel = 10,
						    randomSeed = 12345;
	public static void main(String[] args) {
		DepCorpus trainCorpus = new DepCorpus("en-universal-train");
		try {
			trainCorpus.loadCoNLL(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		int[] samples = RandomSampler.sampleIDs(maxSentenceID, numToLabel,
				 								randomSeed);
		ArrayList<JSONObject> jsonSentences = new ArrayList<JSONObject>();
		for (int id : samples) {
			DepSentence sentence = trainCorpus.sentences.get(id);
			jsonSentences.add(sentence.toJSON());
		}
		JSONObject data = new JSONObject();
		try {
			data.put("sentences", jsonSentences);
			System.out.println(data.toString(4));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
