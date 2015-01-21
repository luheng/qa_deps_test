package experiments;

import java.util.ArrayList;

import data.AnnotatedSentence;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;
import evaluation.F1Metric;

/**
 * To validate SRL annotation (pilot study).
 * @author luheng
 *
 */
public class SRLAnnotationValidator {

	// Evaluation settings.
	private static boolean ignoreRootPropArcs = false;
	private static boolean ignoreNominalArcs = true;
	private static boolean ignoreAmModArcs = true;
	private static boolean ignoreAmAdvArcs = false;
	private static boolean ignoreAmNegArcs = true;
	
	private static boolean coreArgsOnly = true;
	
	private static boolean containedInAnswer(int idx, int[] answerAlignment) {
		for (int answerIdx : answerAlignment) {
			if (answerIdx == idx) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isWhoWhatQuestion(QAPair qa) {
		String qword = qa.questionTokens[0];
		return qword.equalsIgnoreCase("who") || qword.equals("whom") ||
			   qword.equalsIgnoreCase("what");
	}
	
	// Accuracy: +1 if answer span contains real answer head
	// Only count verb propositions ...
	public void computeSRLAccuracy(
			ArrayList<AnnotatedSentence> annotatedSentences,
			SRLCorpus corpus) {
		
		F1Metric avgF1 = new F1Metric();
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			SRLSentence srlSentence = (SRLSentence) sentence.depSentence;
			int length = srlSentence.length + 1;
			String[][] goldArcs = srlSentence.getSemanticArcs();
			boolean[][] covered = new boolean[length][length];
			int numUncoveredGoldArcs = 0, // Recall loss.
				numUnmatchedPredSpans = 0, // Precision loss.
				numGoldArcs = 0;
	
			for (int i = 0; i < length; i++) {
				for (int j = 1; j < length; j++) {
					if (ignoreRootPropArcs && i == 0) {
						goldArcs[i][j] = "";
					} 
					if (ignoreNominalArcs) {
						if ((i == 0 && !srlSentence.getPostagString(j - 1)
						   		.equals("VERB")) ||
						   	(i > 0 && !srlSentence.getPostagString(i - 1)
							   		.equals("VERB"))) {
							goldArcs[i][j] = "";
						}
					}
					
					if (coreArgsOnly && goldArcs[i][j].startsWith("AM")) {
						goldArcs[i][j] = "";
					}
					
					if (ignoreAmModArcs &&
							goldArcs[i][j].equals("AM-MOD")) {
						goldArcs[i][j] = "";
					}
					if (ignoreAmAdvArcs &&
							goldArcs[i][j].equals("AM-ADV")) {
						goldArcs[i][j] = "";
					}
					if (ignoreAmNegArcs &&
							goldArcs[i][j].equals("AM-NEG")) {
						goldArcs[i][j] = "";
					}
					
					if (!goldArcs[i][j].isEmpty()) {
						numGoldArcs ++;
						numUncoveredGoldArcs ++;
						covered[i][j] = false;
					}
				}
			}
		
			System.out.println(srlSentence.sentenceID + "\t" +
							   srlSentence.getTokensString());
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
			
			System.out.println("[Precision loss]:");
			
			for (QAPair qa : sentence.qaList) {
				// If the answer is the "secondary answer" provided by annotator
				if (qa.mainQA != null) {
					continue;
				}
				if (coreArgsOnly && !isWhoWhatQuestion(qa)) {
					continue;
				}
				
				int propHead = qa.getPropositionHead() + 1;
				assert (propHead > 0);
				
				if (!ignoreRootPropArcs) {
					if (!goldArcs[0][propHead].isEmpty() &&
						!covered[0][propHead]) {
						numUncoveredGoldArcs --;
						covered[0][propHead] = true;
					}
				}
				
				boolean matchedGold = false;
				for (int argHead = 1; argHead < length; argHead++) {
					if (goldArcs[propHead][argHead] != "") {
						if (containedInAnswer(argHead - 1, qa.answerAlignment)) {
							if (!covered[propHead][argHead]) {
								numUncoveredGoldArcs --;
								covered[propHead][argHead] = true;
							}
							matchedGold = true;
						}
					}
				}
				if (!matchedGold) {
					++ numUnmatchedPredSpans;
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
				}
			}
			
			// Output recall loss
			int numMatchedArcs = numGoldArcs - numUncoveredGoldArcs;
			F1Metric f1 = new F1Metric(numMatchedArcs,
									   numGoldArcs,
									   numMatchedArcs + numUnmatchedPredSpans);

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
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrialFilename, "en-srl-trial");
		ArrayList<AnnotatedSentence> annotatedSentences =
				ExperimentUtils.loadSRLAnnotationSentences(trainCorpus);
	
		// TODO: validate SRL (unlabeled) accuracy.
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}