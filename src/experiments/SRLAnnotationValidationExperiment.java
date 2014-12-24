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
				//numUnmatchedPredSpans = 0, // Precision loss.
				numGoldArcs = 0;
			
			for (int i = 0; i < length; i++) {
				for (int j = 0; j < length; j++) {
					if (i > 0 &&
						!srlSentence.getPostagString(i - 1).equals("VERB")) {
						goldArcs[i][j] = "";
					} else if (!goldArcs[i][j].isEmpty()) {
						numGoldArcs ++;
						numUncoveredGoldArcs ++;
						covered[i][j] = false;
					}
				}
			}
			
			for (QAPair qa : sentence.qaList) {
				int propHead = qa.getPropositionHead();
				assert (propHead != -1);
				boolean matchedGold = false;
				for (int i = 0; i < length; i++) {
					if (goldArcs[propHead + 1][i] != "") {
						if (!covered[propHead + 1][i] &&
							containedInAnswer(i - 1, qa.answerAlignment)) {
							//System.out.println(
							//	srlSentence.getTokenString(i - 1) + "," +
							//	goldArcs[propHead + 1][i]);
							numUncoveredGoldArcs --;
							covered[propHead + 1][i] = true;
							matchedGold = true;
							break;
						}
					}
				}
				if (!matchedGold) {
					//++ numUnmatchedPredSpans;
				}
			}
			F1Metric f1 = new F1Metric(numGoldArcs - numUncoveredGoldArcs,
									   numGoldArcs,
									   sentence.qaList.size());
			System.out.print(sentence.toString());
			System.out.println(f1.toString() + "\n");
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