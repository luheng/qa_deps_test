package experiments;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import data.DepCorpus;
import data.DepSentence;

public class DataPreparationExperiment {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static void main(String[] args) {
		DepCorpus trainCorpus = new DepCorpus("en-universal-train");
		try {
			trainCorpus.loadCoNLL(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		DepSentence sentence = trainCorpus.sentences.get(0);
		JSONObject jsonSent = sentence.toJSON();
		try {
			System.out.println(jsonSent.toString(4));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
