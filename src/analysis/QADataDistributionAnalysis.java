package analysis;

import java.util.HashSet;

import data.Corpus;
import learning.AnswerIdDataset;

public class QADataDistributionAnalysis {
	 
	// TODO: vocab size, nr. ood words, sentence length, avg nr. propositions, avg. nr qas
	public static void analyze(AnswerIdDataset ds) {
		Corpus corpus = ds.getCorpus();
		System.out.println(String.format("Dataset: %s; Base corpus: %s",
				ds.datasetName, corpus.corpusName));

		// *************** Vocabulary size ****************
		HashSet<Integer> sentIds = ds.getSentenceIds();
		System.out.println(String.format("Vocab size:\t%d" ,
				corpus.wordDict.size()));

		// *************** Averaged sentence length ************
		double avgSentLength = 0.0;
		for (int sentId : sentIds) {
			avgSentLength += corpus.getSentence(sentId).length;
			
		}
		avgSentLength /= sentIds.size();
		System.out.println(String.format("Averaged sentence length:\t %.2f",
				avgSentLength));

	}
	
	public static void analyzeOOD(AnswerIdDataset inDomainDs,
								  AnswerIdDataset outOfDomainDs) {
		
	}
}
