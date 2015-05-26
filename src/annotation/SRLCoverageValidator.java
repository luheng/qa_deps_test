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
import evaluation.Accuracy;

public class SRLCoverageValidator {
	public boolean ignoreNominalArcs = true;
	public boolean ignoreAmModArcs = true;
	public boolean ignoreAmDisArcs = true;
	public boolean ignoreAmAdvArcs = false;
	public boolean ignoreAmNegArcs = true;
	public boolean ignoreRAxArcs = true;

	public boolean goldPropositionOnly = true; 
	public boolean coreArgsOnly = false;
	public boolean nonCoreArgsOnly = false;
	
	// So if the gold argument head has a child that is contained in the answer
	// span, we say there is a match.
	private static boolean allowTwoHopValidation = true;
	
	private static boolean hasChildInAnswer(int idx, int[] flags,
			DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			if (flags[i] > 0 && sentence.parents[i] == idx) {
				return true;
			}
		}
		return false;
	}
	
	public void getGoldSRL(SRLSentence sentence, String[][] goldArcs,
			int[][] continuation) {
		int length = sentence.length + 1;
		String[][] arcs = sentence.getSemanticArcs();
		for (int i = 0; i < length; i++) {
			for (int j = 1; j < length; j++) {
				goldArcs[i][j] = arcs[i][j];
				continuation[i][j] = 0;
				if (ignoreNominalArcs) {
					if ((i == 0 && !sentence.getPostagString(j - 1).equals("VERB")) ||
					   	(i > 0 && !sentence.getPostagString(i - 1).equals("VERB"))) {
						goldArcs[i][j] = "";
					}
				}
				if (i == 0) {
					continue;
				}
				if (nonCoreArgsOnly && !goldArcs[i][j].startsWith("AM") &&
						!goldArcs[i][j].startsWith("C-AM")) {
					goldArcs[i][j] = "";
				}
				if (coreArgsOnly && (goldArcs[i][j].startsWith("AM") ||
						 goldArcs[i][j].startsWith("C-AM"))) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmModArcs && goldArcs[i][j].contains("AM-MOD")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmDisArcs && goldArcs[i][j].contains("AM-DIS")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmAdvArcs && goldArcs[i][j].contains("AM-ADV")) {
					goldArcs[i][j] = "";
				}
				if (ignoreAmNegArcs && goldArcs[i][j].contains("AM-NEG")) {
					goldArcs[i][j] = "";
				}
				if (ignoreRAxArcs && goldArcs[i][j].startsWith("R-A")) {
					goldArcs[i][j] = "";
				}
			}
		}
		for (int i = 1; i < length; i++) {
			for (int j = 1; j < length; j++) {
				if (goldArcs[i][j].startsWith("C-A")) {
					String arg = goldArcs[i][j].substring(2);
					int k = 1;
					for (; k < length; k++) {
						if (goldArcs[i][k].equals(arg)) {
							break;
						}
					}
					continuation[i][j] = k;
				}
			}
		}
	}
	
	public boolean matchedGold(int goldArgHead, QAPair qa,
			SRLSentence srlSentence) {
		boolean headInAnswer = (qa.answerFlags[goldArgHead] > 0);
		boolean childInAnswer = hasChildInAnswer(goldArgHead, qa.answerFlags, srlSentence);
		String argHeadPos = srlSentence.getPostagString(goldArgHead);
		boolean headIsPP = argHeadPos.equals("ADP") || argHeadPos.equals("PRT");
		return (headInAnswer || (allowTwoHopValidation && childInAnswer && headIsPP));
	}
	
	private CountDictionary getQLabels(
			Collection<AnnotatedSentence> annotatedSentences) {
		CountDictionary labelDict = new CountDictionary();
		for (AnnotatedSentence annotSent : annotatedSentences) {
			for (int propHead : annotSent.qaLists.keySet()) {
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					//String ql = qa.questionLabel.split("=")[0].split("_")[0];
					String ql = qa.questionWords[0].toUpperCase();
					labelDict.addString(ql);
				}
			}
		}
		return labelDict;
	}
	
	private void computeAverage(String label, ArrayList<Accuracy> res) {
		Accuracy micro = new Accuracy();
		double macro = 0.0;
		for (int i = 0; i < res.size(); i++) {
			micro.add(res.get(i));
			macro += res.get(i).accuracy();
		}
		macro /= res.size();
		System.out.println(String.format("%s\tmicro:\t%.3f\tmacro: %.3f",
				label, micro.accuracy(), macro));
	}
	public void computeCoverage(Collection<AnnotatedSentence> annotations,
			SRLCorpus corpus) {
		CountDictionary qlabelDict = getQLabels(annotations);
		int[][] labelMap = new int[corpus.argModDict.size()][qlabelDict.size()];
		int[] goldCnt = new int[corpus.argModDict.size()];
		Arrays.fill(goldCnt, 0);
		for (int i = 0; i < labelMap.length; i++) {
			Arrays.fill(labelMap[i], 0);
		}
		
		// Percentage of QAs that cover at least one SRL relation.
		ArrayList<Accuracy> qaCoversSRL= new ArrayList<Accuracy>();
		// Percentage of QAs taht cover exactly one SRL relation.
		ArrayList<Accuracy> qaCoversOneSRL = new  ArrayList<Accuracy>();
		// Percentage of SRL relations that is covered by at least one QA.
		ArrayList<Accuracy> srlCoveredByQA = new ArrayList<Accuracy>();
		// Percentage of SRL relations that is covered by exactly one QA.
		ArrayList<Accuracy> srlCoveredByOneQA = new ArrayList<Accuracy>();
		
		for (AnnotatedSentence sent : annotations) {	
			SRLSentence srlSentence = (SRLSentence) sent.sentence;
			int length = srlSentence.length + 1;
			String[][] goldArcs = new String[length][length];
			int[][] cont = new int[length][length];
			getGoldSRL(srlSentence, goldArcs, cont);
			// Go over all propositions
			for (int propId : sent.qaLists.keySet()) {
				int propHead = propId + 1;
				if (goldPropositionOnly && goldArcs[0][propHead].isEmpty()) {
					continue;
				}
				int[] covered = new int[length];
				Arrays.fill(covered, 0);
				Accuracy q1 = new Accuracy(), q2 = new Accuracy(),
						 s1 = new Accuracy(), s2 = new Accuracy();
				ArrayList<QAPair> qaList = sent.qaLists.get(propId);
				for (QAPair qa : qaList) {
					// String qlabel = qa.questionLabel.split("=")[0].split("_")[0];
					//boolean coreQA = (qlabel.equals("W0") ||
					//		qlabel.equals("W1") || qlabel.equals("W2"));
					String qlabel = qa.questionWords[0].toUpperCase();
					boolean coreQA = (qlabel.equals("WHO") || qlabel.equals("WHAT"));
					if (coreArgsOnly && !coreQA) {
						continue;
					} else if (nonCoreArgsOnly && coreQA) {
						continue;
					}
					boolean[] matched = new boolean[length];
					Arrays.fill(matched, false);
					for (int argHead = 1; argHead < length; argHead++) {
						String goldLabel = goldArcs[propHead][argHead];
						if (goldLabel.isEmpty()) {
							continue;
						}
						if (matchedGold(argHead - 1, qa, srlSentence)) {
							int a = argHead;
							if (cont[propHead][a] > 0) {
								a = cont[propHead][a];
							}
							covered[a] ++;
							matched[a] = true;
							int goldLabelId = corpus.argModDict.lookupString(
									goldArcs[propHead][a]);
							int qlabelId = qlabelDict.lookupString(qlabel);
							labelMap[goldLabelId][qlabelId] ++;	
						}
					}
					int matchedGold = 0;
					for (boolean m : matched) {
						matchedGold += (m ? 1 : 0);
					}
					q1.numCounted ++;
					q2.numCounted ++;
					q1.numMatched += (matchedGold == 1 ? 1 : 0);
					q2.numMatched += (matchedGold > 0 ? 1 : 0);
				}
				// Count gold.
				//int cnt = 0;
				for (int argHead = 1; argHead < length; argHead++) {
					String goldLabel = goldArcs[propHead][argHead];
					if (!goldLabel.isEmpty() && cont[propHead][argHead] == 0) {
						s1.numCounted ++;
						s2.numCounted ++;
						//cnt ++;
					}
				}
				//if (cnt == 0 && !goldArcs[0][propHead].isEmpty()) {
				//	System.out.println(srlSentence.toString() + ", "
				//			+ srlSentence.getTokenString(propHead - 1));
				//} 
				for (int i = 1; i < length; i++) {
					s1.numMatched += (covered[i] == 1 ? 1 : 0);
					s2.numMatched += (covered[i] > 0 ? 1 : 0);
				}
				if (q1.numCounted > 0) {
					qaCoversOneSRL.add(q1);
					qaCoversSRL.add(q2);					
				}
				if (s1.numCounted > 0) {
					srlCoveredByOneQA.add(s1);
					srlCoveredByQA.add(s2);
				}
			}
		}
		MatrixPrinter.prettyPrint(labelMap, corpus.argModDict, qlabelDict);
		MatrixPrinter.prettyPrint(goldCnt, corpus.argModDict);
		
		// Compute coverage numbers.
		computeAverage("qaCoversOneSRL", qaCoversOneSRL);
		computeAverage("qaCoversSRL", qaCoversSRL);
		computeAverage("srlCoveredByOneQA", srlCoveredByOneQA);
		computeAverage("srlCoveredByQA", srlCoveredByQA);
	}
}
