package experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import util.DebugUtils;
import util.LatticeUtils;
import annotation.AuxiliaryVerbIdentifier;
import annotation.BasicQuestionTemplates;
import annotation.QuestionTemplate;
import annotation.CandidateProposition;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.DepSentence;
import data.QAPair;
import data.SRLSentence;
import evaluation.DependencySRLEvaluation;
import evaluation.F1Metric;

/**
 * Interactive annotation using the workflow:
 * W-Rank -> Q-Gen -> A-Align -> Update
 * @author luheng
 *
 */
public class InteractiveAnnotationExperiment {
	
	private static DepCorpus trainCorpus = null;
	// private static ArrayList<AnnotatedSentence> annotatedSentences = null;
	
	private static InteractiveConsole console = null;
	// private static int[] sentenceIDs =
	//		new int[] {6251, 9080, 8241, 8828, 55};
	
	private static ArrayList<CandidateProposition> getCandidatePropositions(
			DepSentence sentence) {
		// Pre-processing: get auxiliary verb spans.
		AuxiliaryVerbIdentifier auxVerbIdentifier =
				new AuxiliaryVerbIdentifier(trainCorpus);
		int[] verbHeads = new int[sentence.length];
		auxVerbIdentifier.process(sentence, verbHeads);
		
		ArrayList<CandidateProposition> propositions =
				new ArrayList<CandidateProposition>();
		for (int i = 0; i < sentence.length; i++) {
			// Compute score ...
			String postag = sentence.getPostagString(i);
			if (verbHeads[i] != -1) {
				// Is a group of auxiliary verbs and verbs.
				CandidateProposition word = new CandidateProposition(sentence,
						i, new int[] {i, verbHeads[i] + 1});
				word.score = 3;
				propositions.add(word);
				i = verbHeads[i];
				continue;
			}
			CandidateProposition word = new CandidateProposition(sentence, i);
			if (postag.equals("VERB")) {
				word.score = 2;
			} else if (postag.equals("NOUN")) {
				word.score = 1;
			} else {
				word.score = -1;
			}
			propositions.add(word);
 		}
		Collections.sort(propositions, CandidateProposition.comparator);
		return propositions;
	}
	
	private static void showNumberedSentence(DepSentence sentence,
											 int maxNumTokensPerLine) {
		int counter = 0;
		for (int i = 0; i < sentence.length; i++) {
			System.out.print(String.format("%s(%d) ",
					sentence.getTokenString(i), i));
			if (++counter == maxNumTokensPerLine) {
				System.out.println();
				counter = 0;
			}
		}
		System.out.println();
	}
	
	/**
	 * Parse input answer span string into [a, b]. return [-1, -1] if failed.
	 * @param inputStr
	 * @return
	 */
	private static ArrayList<int[]> processAnswerSpan(String inputStr) {
		ArrayList<int[]> spans = new ArrayList<int[]>();
		if (!inputStr.isEmpty()) {
			String[] spanStrs = inputStr.split(",");
			for (String spanStr : spanStrs) {
				int split = spanStr.indexOf('-');
				if (split > 0) {
					try {
						int a = Integer.parseInt(spanStr.substring(0, split));
						int b = Integer.parseInt(spanStr.substring(split + 1));
						// Add one for the convention ...
						int[] span = new int[] {a, b + 1};
						spans.add(span);
					} catch (NumberFormatException e) {
					}
				} else {
					try {
						int a = Integer.parseInt(spanStr);
						int[] span = new int[] {a, a + 1};
						spans.add(span);
					} catch (NumberFormatException e) {
					}
				}
			}
			return spans;
		}
		int[] span = new int[] {-1, -1};
		spans.add(span);
		return spans;
	}
	
	private static boolean insideSpan(int[] smallerSpan, int[] largerSpan) {
		return smallerSpan[0] >= largerSpan[0] &&
			   smallerSpan[1] <= largerSpan[1];
	}
	
	// For each answer span, the word that has outcoming arc is the head of the
	// argument.
	// FIXME: Temporary solution. This is inefficient.
	private static int getAnswerHead(QAPair qa, int[] tree) { 
		HashSet<Integer> answerIDs = new HashSet<Integer>();
		for (int i:  qa.answerAlignment) {
			if (i != -1) {
				answerIDs.add(i);
			}
		}
		int answerHead = -1;
		for (int i : qa.answerAlignment) {
			if (i != -1) {
				if (!answerIDs.contains(tree[i])) {
					answerHead = i;
				}
			}
		}
		return answerHead;
	}
	
	private static int getQuestionHead(QAPair qa) {
		for (int i : qa.questionAlignment) {
			if (i != -1) {
				return i;
			}
		}
		return -1;
	}
	
	private static void updateSemanticArcs(String[][] semPred, int[] synPred,
			AnnotatedSentence annotatedSentence) {
		LatticeUtils.fill(semPred, "");
		SRLSentence sentence = (SRLSentence) annotatedSentence.depSentence;
		for (QAPair qa : annotatedSentence.qaList) {
			int propID = getQuestionHead(qa);
			int argID = getAnswerHead(qa, synPred);
			if (propID < 0 || argID < 0) {
				continue;
			}
			semPred[0][propID + 1] = sentence.getTokenString(propID);
			semPred[propID + 1][argID + 1] = "A?";
		}
	}
	
	private static void annotateSentence(SRLSentence sentence) {
		// Get semantic arcs for evaluation.
		String[][] semPred =
				new String[sentence.length + 1][sentence.length + 1];
		String[][] semGold =
				sentence.getSemanticArcs();
		DebugUtils.printSemanticArcs(sentence, semGold);
		
		// Initialize annotation data structure.
		AnnotatedSentence annotatedSentence = new AnnotatedSentence(sentence);
		
		// Extract candidate propositions.
		ArrayList<CandidateProposition> candidates =
				getCandidatePropositions(sentence);
		BasicQuestionTemplates questionTemplates = new BasicQuestionTemplates();
		
		for (CandidateProposition word : candidates) {
			System.out.println(word.toString());
		}
		boolean[] tried = new boolean[sentence.length];
		Arrays.fill(tried, false);

		System.out.println(
				"Input answer number (a) or range (a-b), " +
				"if there are multiple answers, delimit by comma. " +
				"Enter empty string if there is no answer applied.\n");
		
		showNumberedSentence(sentence, 20);
		
		System.out.println();
		int questionCounter = 0;
		
		while (true) {
			// Retrieve highest ranked word in the current span.
			CandidateProposition qWord = null;
			for (CandidateProposition word : candidates) {
				if (!tried[word.wordID] && word.score > -1) {
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
				/*
				System.out.println("q-gen:\t" +
						qtemp.getQuestionString(sentence, qWord));
				*/
				if (qtemp.matches(sentence,
								  qWord,
								  qWord.effectiveSpan)) {
					
					// Show numbered sentence and question.
					System.out.print(
							String.format("Question %d:\t", ++questionCounter));
					System.out.println(
							qtemp.getNumberedQuestionString(sentence,
									qWord));
					String inputStr = console.readLine(
							"Input answer:\t");
					ArrayList<int[]> answerSpans =
							processAnswerSpan(inputStr.trim());
					
					// Confirm answer ..
					if (answerSpans.get(0)[0] == -1) {
						continue;
					}
					
					// Generate question and answer strings to form QA pair.
					String question = qtemp.getQuestionString(sentence, qWord);
					for (int[] answerSpan : answerSpans) {
						String answer = sentence.getTokenString(answerSpan);
						System.out.println("Answer:\t" + answer);
					
						// Create new QA pair.
						QAPair qa = new QAPair(question, answer);
						int questionPtr = qtemp.getSlotID();
						for (int i = qWord.wordSpan[0]; i < qWord.wordSpan[1];
								i++) {
							qa.questionAlignment[questionPtr++] = i;
						}
				
						int answerPtr = 0;
						for (int i = answerSpan[0]; i < answerSpan[1]; i++) {
							qa.answerAlignment[answerPtr++] = i;
						}
						
						// Print QA alignment
						/*
						qa.printAlignment();
						*/
						annotatedSentence.addQA(qa);
					}
				
					// Update effective spans of question words.
					for (CandidateProposition word : candidates) {
						if (word.wordID == qWord.wordID) {
							// Do not want to shrink on the current question
							// word, yet.
							continue;
						}
						for (int[] span : answerSpans) {
							if (insideSpan(word.wordSpan, span)) {
								word.effectiveSpan[0] =
									Math.max(word.effectiveSpan[0], span[0]);
								word.effectiveSpan[1] =
									Math.min(word.effectiveSpan[1], span[1]);
							}
						}
					}
					foundQuestion = true;
				}
			}

			tried[qWord.wordID] = true;
			if (foundQuestion) {
				// Compute accuracy.
				int[] synPred = CombinedScorerExperiment.testSentence(
						annotatedSentence,
						0.0, /* weight of distance scorer */
						1.0, /* weight of QA scorer */
						1.0,  /* weight of UG scorer */
						true /* print analysis */);
				
				// Update semantic prediction.
				updateSemanticArcs(semPred, synPred, annotatedSentence);
				F1Metric f1 = DependencySRLEvaluation.getUnlabeledAccuracy(
						sentence, semPred);
				System.out.println(f1.toString());
				DebugUtils.printSemanticArcs(sentence, semPred);
				
				// Update on word ranks.
				Collections.sort(candidates, CandidateProposition.comparator);
			}
		}
	}
	
	public static void main(String[] args) {
		// trainCorpus = ExperimentUtils.loadDepCorpus();
		trainCorpus = ExperimentUtils.loadSRLCorpus();
		
		// Interaction. Try the first sentence.
		console = new InteractiveConsole();
		annotateSentence((SRLSentence) trainCorpus.sentences.get(0));
	}
}