package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import data.CountDictionary;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;

public class GenerateSRLEmbeddingData {

	private static String vocabPath = "embedding_data/vocab_count.txt";
	private static String contextTrainPath = "embedding_data/context_train.txt";
	
	//private static boolean ignorePunctuations = true;
	private static boolean ignoreNominalPredicates = true;
	private static boolean lowerCaseWords = true;
	
	private static CountDictionary outputVocabCounts(SRLCorpus corpus)
			throws IOException {
		CountDictionary vocab = new CountDictionary();
		for (int i = 0; i < corpus.wordDict.size(); i++) {
			String word = corpus.wordDict.getString(i);
			if (lowerCaseWords) {
				word = word.toLowerCase();
			}
			int count = corpus.wordDict.getCount(i);
			vocab.addString(word, count);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(vocabPath));
		for (int i = 0; i < vocab.size(); i++) {
			writer.write(String.format("%s\t%d\n", vocab.getString(i),
					vocab.getCount(i)));
		}
		writer.close();
		System.out.println(String.format("%d words are written to %s.",
				vocab.size(), vocabPath));
		return vocab;
	}
	
	private static void outputWordContextPairs(SRLCorpus corpus,
			CountDictionary vocab) throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(contextTrainPath));
		int lineCounter = 0;
		
		for (int i = 0; i < corpus.sentences.size(); i++) {
			SRLSentence sent = (SRLSentence) corpus.sentences.get(i);
			for (Proposition prop : sent.propositions) {
				int propHead = prop.propID;
				if (ignoreNominalPredicates &&
					!sent.getPostagString(propHead).equals("VERB")) {
					continue;
				}
				String propWord = sent.getTokenString(propHead);
				
				for (int j = 0; j < prop.argIDs.size(); j++) {
					String argWord = sent.getTokenString(prop.argIDs.get(j));
					String relType = corpus.argModDict.getString(
							prop.argTypes.get(j));
					
					if (lowerCaseWords) {
						propWord = propWord.toLowerCase();
						argWord = argWord.toLowerCase();
					}
					writer.write(String.format("%s\t%s_%s\n",
							propWord, relType, argWord));
					writer.write(String.format("%s\t%sI_%s\n",
							argWord, relType, propWord));
					lineCounter += 2;
				}
			}
		}
		writer.close();
		System.out.println(String.format("%d word-context pairs are written to %s.",
				lineCounter, contextTrainPath));
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		CountDictionary vocab = null;
		
		try {
			vocab = outputVocabCounts(trainCorpus);
			outputWordContextPairs(trainCorpus, vocab);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
