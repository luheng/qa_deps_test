package experiments;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import util.StringUtils;
import annotation.AuxiliaryVerbIdentifier;
import annotation.BasicQuestionTemplates;
import annotation.QuestionTemplate;
import annotation.WordRank;
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
			if (w1.score > w2.score + 1e-8) {
				return -1;
			}
			return w1.score + 1e-8 < w2.score ? 1 : 0;
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
		
		// Pre-processing: get auxiliary verb spans.
		AuxiliaryVerbIdentifier auxVerbIdentifier =
				new AuxiliaryVerbIdentifier(trainCorpus);
		int[] verbHeads = new int[sentence.length];
		auxVerbIdentifier.process(sentence, verbHeads);
		
		for (int i = 0; i < sentence.length; i++) {
			// Compute score ...
			String postag = sentence.getPostagString(i);
			if (verbHeads[i] != -1) {
				// Is a group of auxiliary verbs and verbs.
				WordRank word = new WordRank(sentence, i,
						new int[] {i, verbHeads[i] + 1});
				word.score = 3.5;
				wordRanks.add(word);
				i = verbHeads[i];
				continue;
			}
			WordRank word = new WordRank(sentence, i);
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
		boolean[] asked = new boolean[sentence.length];
		Arrays.fill(asked, false);
		int[] currentSpan = new int[] {0, sentence.length};
		int currentQA = -1;
		// FIXME: Auxiliary verb problem when extracting verbs.
		// FIXME: Print numbered sentence in a better way.
		showNumberedSentence(sentence);
		
		while (true) {
			// Retrieve highest ranked word in the current span.
			// int wordID = wordRanks.get(0).wordID;
			
			WordRank currentWord = null;
			for (WordRank word : wordRanks) {
				if (!asked[word.wordID] && word.score > -1 &&
					word.wordSpan[0] >= currentSpan[0] &&
					word.wordSpan[1] < currentSpan[1]) {
					currentWord = word;
					break;
				}
			}
			if (currentWord == null) {
				System.out.println("All words processed");
				break;
			}
			
			// Suggest word + wh-word combination.
			boolean foundQuestion = false;
			for (QuestionTemplate qtemp : questionTemplates.templates) {
				// System.out.println("q-gen:\t" + qtemp.getQuestionString(sentence, wordID));
				if (qtemp.matches(sentence, currentWord, currentSpan)) {
					// Show numbered sentence and question.
					System.out.println(
							qtemp.getNumberedQuestionString(sentence,
									currentWord));
					String inputStr = console.readLine(
							"Input answer range (a-b), 0 for no answer:");
					int[] answerSpan = processAnswerSpan(inputStr);
					
					// Confirm answer ..
					if (answerSpan[0] == -1) {
						continue;
					}
					String question = qtemp.getQuestionString(sentence,
							currentWord);
					String answer = getAnswerSpanString(sentence, answerSpan);
					System.out.println("Answer:\t" + answer);
					
					// Create new QA pair.
					QAPair qa = new QAPair(question, answer);
					for (int i = currentWord.wordSpan[0], j = qtemp.getSlotID();
							i < currentWord.wordSpan[1]; i++, j++) {
						qa.questionAlignment[j] = i;
					}
					
					for (int i = answerSpan[0]; i < answerSpan[1]; i++) {
						qa.answerAlignment[i - answerSpan[0]] = i;
					}
					// Print QA alignment
					System.out.println(StringUtils.intArrayToString(" ", qa.questionAlignment));
					System.out.println(StringUtils.intArrayToString(" ", qa.answerAlignment));
					
					annotatedSentence.addQA(qa);
					foundQuestion = true;
				}
			}

			// Move onto the next word;
			asked[currentWord.wordID] = true;
			if (foundQuestion) {
				currentQA ++;
				int[] ans = annotatedSentence.qaList.get(currentQA).answerAlignment;
				currentSpan[0] = ans[0];
				currentSpan[1] = ans[ans.length - 1] + 1;
				System.out.println("current span:\t" + currentSpan[0] + ", " +
									currentSpan[1]);
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