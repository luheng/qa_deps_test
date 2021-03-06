package annotation;

import io.MatrixPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import data.AnnotatedSentence;
import data.CountDictionary;
import data.DepSentence;
import data.SRLCorpus;
import data.SRLSentence;
import data.QAPair;
import evaluation.F1Metric;

public class SRLAnnotationValidator {
	public boolean ignoreNominalArcs = true;
	public boolean ignoreAmModArcs = true;
	public boolean ignoreAmDisArcs = true;
	public boolean ignoreAmAdvArcs = true;
	public boolean ignoreAmNegArcs = true;
	public boolean ignoreRAxArcs = true;

	public boolean goldPropositionOnly = false; 
	public boolean coreArgsOnly = false;
	public boolean ignoreLabels = false;

	public boolean matchOnlyOnce = false;
	
	// So if the gold argument head has a child that is contained in the answer
	// span, we say there is a match.
	private static boolean allowTwoHopValidation = false;
	
	private static boolean hasChildInAnswer(int idx, int[] flags,
			DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			if (flags[i] > 0 && sentence.parents[i] == idx) {
				return true;
			}
		}
		return false;
	}
	
	public String[][] getGoldSRL(SRLSentence sentence) {
		int length = sentence.length + 1;
		String[][] goldArcs = sentence.getSemanticArcs();
		for (int i = 0; i < length; i++) {
			for (int j = 1; j < length; j++) {
				if (ignoreNominalArcs) {
					if ((i == 0 && !sentence.getPostagString(j - 1).equals("VERB")) ||
					   	(i > 0 && !sentence.getPostagString(i - 1).equals("VERB"))) {
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
		boolean childInAnswer = hasChildInAnswer(goldArgHead, qa.answerFlags, srlSentence);
		String argHeadPos = srlSentence.getPostagString(goldArgHead);
		boolean headIsPP = argHeadPos.equals("ADP") || argHeadPos.equals("PRT");
		return (headInAnswer || (allowTwoHopValidation && childInAnswer && headIsPP));
	}
	
	public boolean labelEquals(String goldLabel, String questionLabel) {
		for (int i = 0; i < 6; i++) {
			if (goldLabel.contains("A" + i) && questionLabel.contains("ARG" + i)) {
				return true;
			}
		}
		if (goldLabel.contains("EXT") && questionLabel.contains("HOW MUCH")) {
			return true;
		}
		if (goldLabel.contains("DIR") && questionLabel.contains("WHERE")) {
			return true;
		}
		if (goldLabel.contains("LOC") && questionLabel.contains("WHERE")) {
			return true;
		}
		if (goldLabel.contains("TMP") && questionLabel.contains("WHEN")) {
			return true;
		} 
		if (goldLabel.contains("MNR") && questionLabel.contains("HOW")) {
			return true;
		}
		if (goldLabel.contains("ADV") && questionLabel.contains("HOW")) {
			return true;
		}
		if (goldLabel.contains("PNC") && questionLabel.contains("WHY")) {
			return true;
		}
		return false;
	}
	
	private CountDictionary getQLabels(
			Collection<AnnotatedSentence> annotatedSentences) {
		CountDictionary labelDict = new CountDictionary();
		for (AnnotatedSentence annotSent : annotatedSentences) {
			for (int propHead : annotSent.qaLists.keySet()) {
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String ql = qa.questionLabel.split("=")[0].split("_")[0];
					labelDict.addString(ql);
				}
			}
		}
		return labelDict;
	}
	
	public void computeSRLAccuracy(Collection<AnnotatedSentence> annotations,
			SRLCorpus corpus) {
		F1Metric avgF1 = new F1Metric();
		CountDictionary qlabelDict = getQLabels(annotations);
		int[][] labelMap = new int[corpus.argModDict.size()][qlabelDict.size()];
		int[] goldCnt = new int[corpus.argModDict.size()];
		Arrays.fill(goldCnt, 0);
		for (int i = 0; i < labelMap.length; i++) {
			Arrays.fill(labelMap[i], 0);
		}
		
		for (AnnotatedSentence sent : annotations) {	
			SRLSentence srlSentence = (SRLSentence) sent.sentence;
			int length = srlSentence.length + 1;
			String[][] goldArcs = getGoldSRL(srlSentence); //srlSentence.getSemanticArcs();
			
			boolean[][] covered = new boolean[length][length];
			int numUncoveredGoldArcs = 0, // Recall loss.
				numUnmatchedPredSpans = 0, // Precision loss.
				numGoldArcs = 0;
			
			for (int i = 1; i < length; i++) {
				for (int j = 1; j < length; j++) {
					if (!goldArcs[i][j].isEmpty()) {
						int id = corpus.argModDict.lookupString(goldArcs[i][j]);
						goldCnt[id]++;
						
						if (goldArcs[i][j].startsWith("C-A")) {
							covered[i][j] = true;
						} else {
							numGoldArcs ++;
							numUncoveredGoldArcs ++;
							covered[i][j] = false;
						}
					}
				}
			}
			/*
			System.out.println(srlSentence.sentenceID + "\t" +  srlSentence.getTokensString());
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
				if (goldPropositionOnly && goldArcs[0][propHead].isEmpty()) {
					continue;
				}	
				ArrayList<QAPair> qaList = sent.qaLists.get(propId);
				for (QAPair qa : qaList) {
					String qlabel = qa.questionLabel.split("=")[0].split("_")[0];
					if (coreArgsOnly && !qlabel.startsWith("ARG")) {
						continue;
					}
					boolean matchedGold = false;
					for (int argHead = 1; argHead < length; argHead++) {
						String goldLabel = goldArcs[propHead][argHead];
						if (goldLabel.isEmpty()) {
							continue;
						}
						boolean matchedLabel = (ignoreLabels || labelEquals(goldLabel, qlabel));
						if (matchedGold(argHead - 1, qa, srlSentence) && matchedLabel) {
							if (!covered[propHead][argHead]) {
								numUncoveredGoldArcs --;
								covered[propHead][argHead] = true;
							}
							matchedGold = true;
							int goldLabelId = corpus.argModDict.lookupString(goldLabel);
							int qlabelId = qlabelDict.lookupString(qlabel);
							labelMap[goldLabelId][qlabelId] ++;
							if (matchOnlyOnce) {
								break;
							}
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
		// Output label map.
		MatrixPrinter.prettyPrint(labelMap, corpus.argModDict, qlabelDict);
		MatrixPrinter.prettyPrint(goldCnt, corpus.argModDict);
		
		System.out.println(avgF1.toString());
	}
}
