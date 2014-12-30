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
import data.Proposition;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;
import evaluation.DependencySRLEvaluation;
import evaluation.F1Metric;

/**
 * To validate SRL annotation (pilot study).
 * @author luheng
 *
 */
public class SRLAnnotationValidationExperiment {

	private static SRLCorpus trainCorpus = null;
	private static ArrayList<AnnotatedSentence> annotatedSentences = null;
	
	private static boolean containedInAnswer(int idx, int[] answerAlignment) {
		for (int answerIdx : answerAlignment) {
			if (answerIdx == idx) {
				return true;
			}
		}
		return false;
	}
	
	// Accuracy: +1 if answer span contains real answer head
	// Only count verb propositions ...
	private static void computeSRLAccuracy() {
		 
		F1Metric avgF1 = new F1Metric();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			SRLSentence srlSentence = (SRLSentence) sentence.depSentence;
			int length = srlSentence.length + 1;
			String[][] goldArcs = srlSentence.getSemanticArcs();
			boolean[][] covered = new boolean[length][length];
			int numUncoveredGoldArcs = 0, // Recall loss.
				//numDistinctPred = 0, // Precision loss.
				numGoldArcs = 0;
			
			for (int i = 0; i < length; i++) {
				for (int j = 0; j < length; j++) {
					if (i == 0) {
						// Ignore Root->Proposition arcs.
						goldArcs[i][j] = "";
					}
					else if (!srlSentence.getPostagString(i - 1).equals(
							"VERB") ||
							goldArcs[i][j].equals("AM-MOD") ||
							goldArcs[i][j].equals("AM-ADV")) {
						// Ignore nominal propositions and AM-MOD
						goldArcs[i][j] = "";
					} else if (!goldArcs[i][j].isEmpty()) {
						numGoldArcs ++;
						numUncoveredGoldArcs ++;
						covered[i][j] = false;
					}
				}
			}
			
			System.out.print(sentence.toString());
							   //srlSentence.getTokensString());
			/*
			for (int i = 1; i < length; i++) {
				for (int j = 1; j < length; j++) {
					if (!goldArcs[i][j].isEmpty()) {
						System.out.print(String.format("%s(%d) - %s - %s(%d)\n",
								srlSentence.getTokenString(i - 1), i - 1,
								goldArcs[i][j],
								srlSentence.getTokenString(j - 1), j - 1));
					}
				}
			}
			System.out.println();
			*/
			
			/*
			for (Proposition prop : srlSentence.propositions) {
				System.out.println(prop.toString());
			}
			*/
			 
			System.out.println("[Precision loss]:");
			
			for (QAPair qa : sentence.qaList) {
				int propHead = qa.getPropositionHead();
				assert (propHead != -1);
				boolean matchedGold = false;
				for (int i = 0; i < length; i++) {
					if (goldArcs[propHead + 1][i] != "") {
						if (containedInAnswer(i - 1, qa.answerAlignment)) {
							if (!covered[propHead + 1][i]) {
								numUncoveredGoldArcs --;
								covered[propHead + 1][i] = true;
							}
							matchedGold = true;
						}
					}
				}
				if (!matchedGold) {
					//++ numUnmatchedPredSpans;
					// Output precision loss.
					
					// TODO: remove this big chunk later ...
					System.out.print("[" + propHead + "]\t");
					if (qa.propositionTokens != null) {
						for (int j = 0; j < qa.propositionTokens.length; j++) {
							System.out.print(String.format("%s(%d) ",
									qa.propositionTokens[j],
									qa.propositionAlignment[j]));
						}
						System.out.print("\t");
						for (int j = 0; j < qa.questionTokens.length; j++) {
							if (qa.questionAlignment[j] == -1) {
								System.out.print(qa.questionTokens[j] + " ");
							} else {
								System.out.print(String.format("%s(%d) ",
										qa.questionTokens[j], qa.questionAlignment[j]));
							}
						}
						System.out.print("\t");
						for (int j = 0; j < qa.answerTokens.length; j++) {
							if (qa.answerAlignment[j] == -1) {
								System.out.print(qa.answerTokens[j] + " ");
							} else {
								System.out.print(String.format("%s(%d) ",
										qa.answerTokens[j], qa.answerAlignment[j]));
							}
						}
						System.out.println();
					}
					// Print qa alignment.
				}
			}
			
			// Output recall loss

			F1Metric f1 = new F1Metric(numGoldArcs - numUncoveredGoldArcs,
									   numGoldArcs,
									   sentence.qaList.size());

			if (numUncoveredGoldArcs > 0) {
				System.out.println("[Recall loss]:");
				for (int i = 1; i < length; i++) {
					for (int j = 1; j < length; j++) {
						if (goldArcs[i][j] != "" && !covered[i][j]) {
							System.out.println("\t" +
									srlSentence.getTokenString(i - 1) + ", " +
									srlSentence.getTokenString(j - 1) + ", " +
									goldArcs[i][j]);
						}
					}
				}
			}
			
			System.out.println("[Accuracy]:\t" + f1.toString() + "\n");			
			avgF1.add(f1);
		}
		
		System.out.println(avgF1.toString());
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus();
		annotatedSentences =
				ExperimentUtils.loadSRLAnnotationSentences(trainCorpus);
	
		// TODO: validate SRL (unlabeled) accuracy.
		computeSRLAccuracy();
	}
}