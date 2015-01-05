package experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import annotation.CandidateProposition;
import data.DepSentence;
import data.SRLCorpus;

public class SRLDataPreparationExperiment {

	static SRLCorpus corpus;
	static String outputFileName = "srl.output.tmp";
	
	private static boolean hasPunctuation(DepSentence sentence, int[] span) {
		for (int i = span[0]; i < span[1]; i++) {
			if (sentence.getPostagString(i).equals(".") &&
					!sentence.getTokenString(i).equals("-")) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean getNextSpan(DepSentence sentence, int[] span) {
		int length = sentence.length;
		if (span[0] >= length) {
			return false;
		}
		if (++span[1] >= length) {
			if (++span[0] >= length) {
				return false;
			}
			span[1] = span[0] + 1;
		}
		return true;
	}
	
	/* Each sentence occupy a column. In each column, we enumerate all possible
	 * answer spans (that does not contain a punctuation, except for hyphen).
	 * 
	 */
	private static void printAllAnswerSpans() throws IOException {
		File file =  new File(outputFileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				file.getAbsoluteFile())); 
		
		int maxSentenceLength = 0;
		for (DepSentence sentence : corpus.sentences) {
			if (sentence.length > maxSentenceLength) {
				maxSentenceLength = sentence.length;
			}
		}
		int maxNumAnswerSpans = maxSentenceLength * maxSentenceLength;
		int[][] spans = new int[corpus.sentences.size()][2];
		for (int i = 0; i < spans.length; i++) {
			spans[i][0] = spans[i][1] = 0;
		}
		for (int i = 0; i < maxNumAnswerSpans; i++) {
			boolean flag = false;
			for (int j = 0; j < corpus.sentences.size(); j++) {
				DepSentence sentence = corpus.sentences.get(j);
				writer.write("\t");
				while (getNextSpan(sentence, spans[j])) {
					if (!hasPunctuation(sentence, spans[j]) &&
							spans[j][1] - spans[j][0] < 10) {
						writer.write(sentence.getTokenString(spans[j]));
						flag = true;
						break;
					}
				}
			}
			writer.write("\n");
			if (!flag) {
				break;
			}
		}
		writer.close();
	}
	
	private static void printAllPropositions() {
		for (DepSentence sentence : corpus.sentences) {
			ArrayList<CandidateProposition> props =
					InteractiveAnnotationExperiment.getCandidatePropositions(
							sentence, true /* verb only */);
			// Print sentence.
			System.out.println(String.format("%d\t%s",
					sentence.sentenceID + 1, sentence.getTokensString()));
			System.out.println("[Propositions]");
			// Print propositions.
			for (CandidateProposition prop : props) {
				System.out.println(prop.getPropositionString());
			}
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		corpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrialFilename, "en-srl-trial");
		/*
		for (DepSentence sentence : corpus.sentences) {
			System.out.println(sentence.toString());
		}*/
		/*
		try {
			printAllAnswerSpans();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		printAllPropositions();
	}
}
