package experiments;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import annotation.BasicQuestionTemplates;
import annotation.QuestionTemplate;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.DepSentence;
import data.QAPair;
import decoding.Decoder;
import decoding.ViterbiDecoder;

/**
 * Interactive annotation using the workflow:
 * W-Rank -> Q-Gen -> A-Align -> Update
 * @author luheng
 *
 */
public class InteractiveAnnotationExperiment {
	
	private static DepCorpus trainCorpus = null;
	// private static ArrayList<AnnotatedSentence> annotatedSentences = null;
	private static Decoder decoder = null;
	
	private static InteractiveConsole console = null;
	private static int[] sentenceIDs = new int[] {6251, 9080, 8241, 8828, 55};
	
	private static Comparator<WordRank> wordRankComparator =
			new Comparator<WordRank>() {
		public int compare(WordRank w1, WordRank w2) {
			return (int) (w2.score - w1.score);
		}
	};
	
	private static void showNumberedSentence(DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			System.out.print(String.format("%s(%d) ",
					sentence.getTokenString(i), i));
		}
		System.out.println();
	}
	
	private static String getAnswerSpanString(DepSentence sentence, int[] span) {
		String answerStr = "";
		for (int i = span[0]; i < span[1]; i++) {
			if (i > span[0]) {
				answerStr += " ";
			}
 			answerStr +=  sentence.getTokenString(i);
		}
		return answerStr;
	}
	
	/**
	 * Parse input answer span string into [a, b]. return [-1, -1] if failed.
	 * @param inputStr
	 * @return
	 */
	private static int[] processAnswerSpan(String inputStr) {
		
		int split = inputStr.indexOf('-');
		if (split > 0) {
			try {
				int a = Integer.parseInt(inputStr.substring(0, split));
				int b = Integer.parseInt(inputStr.substring(split + 1));
				// Add one for the convention ...
				return new int[] {a, b + 1};
			} catch (NumberFormatException e) {
			}
		}
		return new int[] {-1, -1};
	}
	
	private static void annotateSentence(DepSentence sentence) {
		// Initialize annotation data structure.
		AnnotatedSentence annotatedSentence = new AnnotatedSentence(sentence);
		
		// Initialize word ranks.
		ArrayList<WordRank> wordRanks =new ArrayList<WordRank>();
		BasicQuestionTemplates questionTemplates = new BasicQuestionTemplates();
		
		for (int i = 0; i < sentence.length; i++) {
			WordRank word = new WordRank(sentence, i);
			// Compute score ...
			String postag = sentence.getPostagString(i); 
			if (postag.equals("VERB")) {
				word.score = 3;
			} else if (postag.equals("NOUN")) {
				word.score = 2;
			} else if (postag.equals("ADP")) {
				word.score = 1;
			} else if (postag.equals("X") || postag.equals(".") ||
					   postag.equals("DET")) {
				word.score = -1;
			}
			wordRanks.add(word);
 		}
		Collections.sort(wordRanks, wordRankComparator);
		for (WordRank word : wordRanks) {
			System.out.println(word.toString());
		}
		int[] currentSpan = new int[] {0, sentence.length};
		
		// FIXME: Auxiliary verb problem when extracting verbs.
		// FIXME: Print numbered sentence in a better way.
		showNumberedSentence(sentence);
		
		for (WordRank word : wordRanks) {
			// Retrieve word from ranked pool.
			// int wordID = wordRanks.get(0).wordID;
			int wordID = word.wordID;
			
			// Suggest word + wh-word combination.
			for (QuestionTemplate qtemp : questionTemplates.templates) {
				if (qtemp.matches(sentence, wordID, currentSpan)) {
					// Show numbered sentence and question.
					System.out.println(
							qtemp.getNumberedQuestion(sentence, wordID));
					String inputStr = console.readLine(
							"Input answer range (a-b), 0 for no answer:");
					int[] answerSpan = processAnswerSpan(inputStr);
					
					// Confirm answer ..
					if (answerSpan[0] == -1) {
						continue;
					}
					String question = qtemp.getQuestion(sentence, wordID);
					String answer = getAnswerSpanString(sentence, answerSpan);
					System.out.print("Answer:\t" + answer);
					
					// Create new QA pair.
					QAPair qa = new QAPair(question, answer);
					// TODO: create unambiguous alignment, yay!
				}
			}

		}
		
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		decoder = new ViterbiDecoder();
		// Do not load annotation yet, haha.
				
		// Interaction. Try the first sentence.
		console = new InteractiveConsole();
		annotateSentence(trainCorpus.sentences.get(sentenceIDs[0]));
	}
	
}

class WordRank {
	public DepSentence sentence;
	public int wordID;
	public double score;
	
	public WordRank(DepSentence sentence, int wordID) {
		this.sentence = sentence;
		this.wordID = wordID;
		this.score = 0.0;
	}
	
	public String toString() {
		return String.format("<%s,%s,%.2f>", sentence.getTokenString(wordID),
				sentence.getPostagString(wordID), score);
	}
}
