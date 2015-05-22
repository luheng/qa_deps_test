package experiments;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import util.RandomSampler;
import util.StrUtils;
import data.DepCorpus;
import data.DepSentence;

public class DataPreparationExperiment {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static final String jsonFilename = 
			"web/en-train-50sentences.json";
	
	public static final String plainTextFilename = 
			"web/en-train-50sentences.txt";
	
	public static final int maxSentenceID = 10000,
						    numToLabel = 50,
						    randomSeed = 12345;
	
	//public static final String outputFormat = "text";
	public static final String outputFormat = "json";
	
	private static void outputJSON(DepCorpus trainCorpus, int[] samples) {
		ArrayList<JSONObject> jsonSentences = new ArrayList<JSONObject>();
		for (int id : samples) {
			DepSentence sentence = trainCorpus.getSentence(id);
			jsonSentences.add(sentence.toJSON());
		}
		JSONObject data = new JSONObject();
		try {
			PrintWriter writer = new PrintWriter(jsonFilename);
			data.put("sentences", jsonSentences);
			writer.println(data.toString(4));
			writer.close();
			System.out.println(String.format("Saved JSON file to: %s",
											 jsonFilename));
		} catch (JSONException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static void outputPlainText(DepCorpus trainCorpus, int[] samples) {
		try {
			@SuppressWarnings("resource")
			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new FileWriter(plainTextFilename)));
			for (int id : samples) {
				DepSentence sentence = trainCorpus.getSentence(id);
				String[] tokens =
						trainCorpus.wordDict.getStringArray(sentence.tokens); 
				writer.print(String.format("%d\t%s\n\n", id,
						StrUtils.join(" ", tokens)));
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		DepCorpus trainCorpus = new DepCorpus("en-universal-train");
		try {
			trainCorpus.loadUniversalDependencyData(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		int[] samples = RandomSampler.sampleIDs(maxSentenceID, numToLabel,
				 								randomSeed);
		// Print sampled sentences.
		for (int id : samples) {
			System.out.print(id + ", ");
		}
		System.out.println();
		
		if (outputFormat.equals("json")) {
			outputJSON(trainCorpus, samples);
		} else {
			outputPlainText(trainCorpus, samples);
		}
	}

}
