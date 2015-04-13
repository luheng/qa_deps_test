package annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import data.AnnotatedSentence;
import data.DepSentence;
import data.SRLCorpus;
import data.SRLSentence;
import data.QAPair;
import util.StringUtils;
import evaluation.F1Metric;

public class SRLAnnotationValidator {
	
	public boolean ignoreRootPropArcs = true;
	public boolean ignoreNominalArcs = true;
	public boolean ignoreAmModArcs = true;
	public boolean ignoreAmDisArcs = true;
	public boolean ignoreAmAdvArcs = false;
	public boolean ignoreAmNegArcs = true;
	public boolean ignoreRAxArcs = true;
	
	public boolean goldPropositionOnly = true; 
	public boolean coreArgsOnly = false;
	
	public boolean ignoreLabels = false;
	
	// So if the gold argument head has a child that is contained in the answer
	// span, we say there is a match.
	private static boolean allowTwoHopValidation = true;
	
	/*
	private static boolean containedInAnswer(int idx, int[][] spans) {
		for (int i = 0; i < spans.length; i++) {
			if (spans[i][0] <= idx && idx < spans[i][1]) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean hasChildInAnswer(int idx, int[][] spans,
											DepSentence sentence) {
		for (int i = 0; i < spans.length; i++) {
			for (int j = spans[i][0]; j < spans[i][1]; j++) {
				if (j >= 0 && sentence.parents[j] == idx) {
					return true;
				}
			}
		}
		return false;
	}
	*/
	
	
	private static boolean hasChildInAnswer(int idx, int[] flags,
			DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			if (flags[i] > 0 && sentence.parents[i] == idx) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isWhoWhatQuestion(QAPair qa) {
		String qstr = qa.getQuestionLabel().toLowerCase();
		return qstr.startsWith("who") || qstr.startsWith("what");
	}
	
	public String[][] getGoldSRL(SRLSentence sentence) {
		int length = sentence.length + 1;
		String[][] goldArcs = sentence.getSemanticArcs();
		
		for (int i = 0; i < length; i++) {
			if (!goldArcs[0][i].isEmpty() &&
				sentence.getPostagString(i - 1).equals("VERB")) {
			}
		}
		
		for (int i = 0; i < length; i++) {
			for (int j = 1; j < length; j++) {
				if (ignoreNominalArcs) {
					if ((i == 0 && !sentence.getPostagString(j - 1)
					   		.equals("VERB")) ||
					   	(i > 0 && !sentence.getPostagString(i - 1)
						   		.equals("VERB"))) {
						goldArcs[i][j] = "";
					}
				}
				if (coreArgsOnly && goldArcs[i][j].startsWith("AM")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmModArcs && goldArcs[i][j].equals("AM-MOD")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmDisArcs && goldArcs[i][j].equals("AM-DIS")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmAdvArcs && goldArcs[i][j].equals("AM-ADV")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmNegArcs && goldArcs[i][j].equals("AM-NEG")) {
					goldArcs[i][j] = "";
				}
				if (ignoreRAxArcs && goldArcs[i][j].startsWith("R-A")) {
					goldArcs[i][j] = "";
				}
			}
		}
		
		return goldArcs;
	}
	
	public boolean matchedGold(int goldArgHead, QAPair qa,
			SRLSentence srlSentence) {
		boolean headInAnswer = (qa.answerFlags[goldArgHead] > 0);
		boolean childInAnswer = hasChildInAnswer(goldArgHead, qa.answerFlags,
				srlSentence);
		String argHeadPos = srlSentence
				.getPostagString(goldArgHead);
		boolean headIsPP = argHeadPos.equals("ADP") ||
				argHeadPos.equals("PRT");
		return (headInAnswer || (allowTwoHopValidation &&
				childInAnswer && headIsPP));
	}
	
	public boolean labelEquals(String goldLabel, String questionLabel) {
		for (int i = 0; i < 6; i++) {
			if (goldLabel.contains("A" + i) && questionLabel.contains("_" + i)) {
				return true;
			}
		}
		if (goldLabel.contains("A2") && questionLabel.contains("do")) {
			return true;
		}
		if (goldLabel.contains("A2") && questionLabel.contains("how much")) {
			return true;
		}
		if (goldLabel.contains("LOC") && questionLabel.contains("where")) {
			return true;
		}
		if (goldLabel.contains("TMP") && questionLabel.contains("when")) {
			return true;
		} 
		if (goldLabel.contains("MNR") && questionLabel.contains("how")) {
			return true;
		}
		return false;
	}
	
	public void computeSRLAccuracy(
			Collection<AnnotatedSentence> annotatedSentences,
			SRLCorpus corpus) {
		
		F1Metric avgF1 = new F1Metric();
		
		for (AnnotatedSentence sent : annotatedSentences) {	
			SRLSentence srlSentence = (SRLSentence) sent.sentence;
			int length = srlSentence.length + 1;
			String[][] goldArcs = srlSentence.getSemanticArcs();
			
			int[] goldProps = new int[srlSentence.length];
			
			boolean[][] covered = new boolean[length][length];
			int numUncoveredGoldArcs = 0, // Recall loss.
				numUnmatchedPredSpans = 0, // Precision loss.
				numGoldArcs = 0;
	
			Arrays.fill(goldProps, 0);
			for (int i = 0; i < length; i++) {
				if (!goldArcs[0][i].isEmpty() &&
					srlSentence.getPostagString(i - 1).equals("VERB")) {
					goldProps[i - 1] = 1;
				}
			}
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
					if (ignoreAmModArcs && goldArcs[i][j].equals("AM-MOD")) {
						goldArcs[i][j] = "";
					}
					if (ignoreAmAdvArcs && goldArcs[i][j].equals("AM-ADV")) {
						goldArcs[i][j] = "";
					}
					if (ignoreAmNegArcs && goldArcs[i][j].equals("AM-NEG")) {
						goldArcs[i][j] = "";
					}
					if (ignoreRAxArcs && goldArcs[i][j].startsWith("R-A")) {
						goldArcs[i][j] = "";
					}
					if (!goldArcs[i][j].isEmpty()) {
						numGoldArcs ++;
						numUncoveredGoldArcs ++;
						covered[i][j] = false;
					}
				}
			}
		
			/*
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
			*/
			
			// Go over all propositions
			for (int propId : sent.qaLists.keySet()) {
				int propHead = propId + 1;

				if (!ignoreRootPropArcs) {
					if (!goldArcs[0][propHead].isEmpty() &&
						!covered[0][propHead]) {
						numUncoveredGoldArcs --;
						covered[0][propHead] = true;
					}
				}
				if (goldPropositionOnly && goldProps[propHead - 1] == 0) {
					continue;
				}
				
		//		System.out.println("#:\t" + srlSentence.getTokenString(propId));
				
				ArrayList<QAPair> qaList = sent.qaLists.get(propId);
				for (QAPair qa : qaList) {
					String qlabel = qa.questionLabel;
					
					if (coreArgsOnly && !isWhoWhatQuestion(qa)) {
						continue;
					}
					boolean matchedGold = false;
					for (int argHead = 1; argHead < length; argHead++) {
						if (goldArcs[propHead][argHead].isEmpty()) {
							continue;
						}
						boolean matchedLabel = (ignoreLabels ||
							labelEquals(goldArcs[propHead][argHead], qlabel));
						if (matchedGold(argHead - 1, qa, srlSentence) &&
							matchedLabel) {
							if (!covered[propHead][argHead]) {
								numUncoveredGoldArcs --;
								covered[propHead][argHead] = true;
							}
							matchedGold = true;
						}
					}
					if (!matchedGold) {
						++ numUnmatchedPredSpans;
						/*
						// TODO: Output precision loss.
						for (int[] ansSpan : qa.answerSpans) {
						System.out.println(
								StringUtils.join(qa.questionWords, " ") +
								" - " +
								sent.sentence.getTokenString(ansSpan));
						}
						*/
					}
					

					// Output QA-pairs, side-by-side from different annotators
					/*
					System.out.println(String.format("%08d\t%s\t%-40s- %s",
							qa.cfAnnotationSources.get(0).cfWorkerId,
							(matchedGold ? "  " : "[*]"), 
							StringUtils.join(" ", qa.questionWords) + "?",
							qa.getAnswerString()));
					*/
				}				
			}
			
			// Output recall loss
			int numMatchedArcs = numGoldArcs - numUncoveredGoldArcs;
			F1Metric f1 = new F1Metric(numMatchedArcs,
									   numGoldArcs,
									   numMatchedArcs + numUnmatchedPredSpans);

			/*
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
			*/
			//System.out.println("[Accuracy]:\t" + f1.toString() + "\n");			
			avgF1.add(f1);
		}
		
		System.out.println(avgF1.toString());
	}
}
