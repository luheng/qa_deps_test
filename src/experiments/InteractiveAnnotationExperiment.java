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
import annotation.QuestionWord;
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
 			answerStr += sentence.getTokenString(i);
		}
		return answerStr;
	}
	
	/**
	 * Parse input answer span string into [a, b]. return [-1, -1] if failed.
	 * @param inputStr
	 * @return
	 */
	private static int[] processAnswerSpan(String inputStr) {
		if (inputStr.isEmpty()) {
			return new int[] {-1, -1};
		}
		int split = inputStr.indexOf('-');
		if (split > 0) {
			try {
				int a = Integer.parseInt(inputStr.substring(0, split));
				int b = Integer.parseInt(inputStr.substring(split + 1));
				// Add one for the convention ...
				return new int[] {a, b + 1};
			} catch (NumberFormatException e) {
			}
		} else {
			try {
				int a = Integer.parseInt(inputStr);
				return new int[] {a, a + 1};
			} catch (NumberFormatException e) {
			}
		}
		return new int[] {-1, -1};
	}
	
	private static boolean insideSpan(int[] smallerSpan, int[] largerSpan) {
		return smallerSpan[0] >= largerSpan[0] &&
			   smallerSpan[1] <= largerSpan[1];
	}
	
	private static void annotateSentence(DepSentence sentence) {
		// Initialize annotation data structure.
		AnnotatedSentence annotatedSentence = new AnnotatedSentence(sentence);
		
		// Initialize word ranks.
		ArrayList<QuestionWord> questionWords =new ArrayList<QuestionWord>();
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
				QuestionWord word = new QuestionWord(sentence, i,
						new int[] {i, verbHeads[i] + 1});
				word.score = 3.5;
				questionWords.add(word);
				i = verbHeads[i];
				continue;
			}
			QuestionWord word = new QuestionWord(sentence, i);
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
			questionWords.add(word);
 		}
		Collections.sort(questionWords, QuestionWord.comparator);
		for (QuestionWord word : questionWords) {
			System.out.println(word.toString());
		}
		boolean[] asked = new boolean[sentence.length];
		Arrays.fill(asked, false);

		// FIXME: Auxiliary verb problem when extracting verbs.
		// FIXME: Print numbered sentence in a better way.
		showNumberedSentence(sentence);
		
		while (true) {
			// Retrieve highest ranked word in the current span.
			// int wordID = wordRanks.get(0).wordID;
			
			QuestionWord qWord = null;
			for (QuestionWord word : questionWords) {
				if (!asked[word.wordID] && word.score > -1) {
					qWord = word;
					break;
				}
			}
			if (qWord == null) {
				System.out.println("All words processed");
				break;
			}
			
			System.out.println(qWord.toString());
			// Suggest word + wh-word combination.
			boolean foundQuestion = false;
			for (QuestionTemplate qtemp : questionTemplates.templates) {
				// System.out.println("q-gen:\t" + qtemp.getQuestionString(sentence, wordID));
				
				if (qtemp.matches(sentence,
								  qWord,
								  qWord.effectiveSpan)) {
					
					// Show numbered sentence and question.
					System.out.println(
							qtemp.getNumberedQuestionString(sentence,
									qWord));
					String inputStr = console.readLine(
							"Input answer nubmer (a) or range (a-b) , " +
							"enter for no answer:");
					int[] answerSpan = processAnswerSpan(inputStr.trim());
					
					// Confirm answer ..
					if (answerSpan[0] == -1) {
						continue;
					}
					String question = qtemp.getQuestionString(sentence,
							qWord);
					String answer = getAnswerSpanString(sentence, answerSpan);
					System.out.println("Answer:\t" + answer);
					
					// Create new QA pair.
					QAPair qa = new QAPair(question, answer);
					for (int i = qWord.wordSpan[0], j = qtemp.getSlotID();
							i < qWord.wordSpan[1]; i++, j++) {
						qa.questionAlignment[j] = i;
					}
					
					for (int i = answerSpan[0]; i < answerSpan[1]; i++) {
						qa.answerAlignment[i - answerSpan[0]] = i;
					}
					// Print QA alignment
					System.out.println(StringUtils.intArrayToString(" ",
							qa.questionAlignment));
					System.out.println(StringUtils.intArrayToString(" ",
							qa.answerAlignment));
					
					// Update effective spans of question words.
					for (QuestionWord word : questionWords) {
						if (insideSpan(word.wordSpan, answerSpan)) {
							word.effectiveSpan[0] =
									Math.max(word.effectiveSpan[0],
											 answerSpan[0]);
							word.effectiveSpan[1] =
									Math.min(word.effectiveSpan[1],
											 answerSpan[1]);
						}
					}
					
					annotatedSentence.addQA(qa);
					foundQuestion = true;
				}
			}

			// Compute accuracy.
			CombinedScorerExperiment.testSentence(annotatedSentence,
					0.0, /* weight of distance scorer */
					1.0, /* weight of QA scorer */
					1.0  /* weight of UG scorer */);
			
			// Update on word ranks.
			asked[qWord.wordID] = true;
			if (foundQuestion) {
				Collections.sort(questionWords, QuestionWord.comparator);
			}
		}
		
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		decoder = new ViterbiDecoder();
		// Do not load annotation yet, haha.
				
		// Interaction. Try the first sentence.
		console = new InteractiveConsole();
		annotateSentence(trainCorpus.sentences.get(sentenceIDs[1]));
	}
	
}