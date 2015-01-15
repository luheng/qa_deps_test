package experiments;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import gnu.trove.list.array.TIntArrayList;
import util.RandomSampler;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;

public class CrowdFlowerPropIdDataPreparation {

	private static SRLCorpus trainCorpus = null; 
	private static final int numUnits = 20;
	private static final int randomPoolSize = 1000;
	
	private static final int randomSeed = 123456;
	
	private static String outputFileName = "crowdflower/CF_PropID_trial.csv";
	
	// Decide if a sentence *contains* a question or not.
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
			if (!isQuestion(sentence) && sentence.length >= 10) {
				ids.add(sentence.sentenceID);
			}
		}
		return ids.toArray();
	}
	
	private static void outputUnits(int[] sentenceIds) throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		CSVFormat csvFormat = CSVFormat.DEFAULT
				.withHeader("SENT_ID", "SENTENCE");
		CSVPrinter csvWriter = new CSVPrinter(fileWriter, csvFormat);
		//csvWriter.printRecord(FILE_HEAEDER);
		for (int i = 0; i < sentenceIds.length; i++) {
			ArrayList<String> record = new ArrayList<String>();
			int sid = sentenceIds[i];
			DepSentence sentence = trainCorpus.sentences.get(sid);
			record.add(String.valueOf(sid));
			//record.add(sentence.getNumberedTokensString());
			record.add(sentence.getTokensString());
			csvWriter.printRecord(record);
		}
		csvWriter.close();
		fileWriter.close();
		System.out.println(String.format(
				"Printed %d sentences into CSV file %s", sentenceIds.length,
				outputFileName));
	}
	
	private static void showVerbPropositions(int[] sentenceIds) {
		double totalNumVerbProps = 0, totalNumArgs = 0; 
		for (int sid : sentenceIds) {
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(sid);
			System.out.println(sentence.sentenceID + ", "
					+ sentence.getTokensString());
			for (Proposition prop : sentence.propositions) {
				int phead = prop.propID;
				if (sentence.getPostagString(phead).equals("VERB")) {
					//System.out.println(phead + ", " +
					//		sentence.getPostagString(phead));
					System.out.println(prop.toString());
					totalNumArgs += prop.getNumArguments();
					++ totalNumVerbProps;
				}
			}
		}
		System.out.println("Averaged number of verb propositions per sentence:"
				+ totalNumVerbProps / sentenceIds.length);
		System.out.println("Averaged number of arguments per proposition:"
				+ totalNumArgs / totalNumVerbProps);
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
	
		int[] nonQuestionIds = getNonQuestionSentenceIds();
		int[] sentenceIds = RandomSampler.sampleIDs(nonQuestionIds,
				randomPoolSize, numUnits, randomSeed);
		
		for (int id : sentenceIds) {
			System.out.print(id + ", ");
		}
		System.out.println();
		/*
		for (int id : sentenceIds) {
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(id);
			System.out.println(sentence.toString());
		}
		*/
		try {
			outputUnits(sentenceIds);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		showVerbPropositions(sentenceIds);
	}
}
