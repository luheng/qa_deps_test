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
	private static boolean insideSpan(int index, int[] span) {
		return span[0] <= index && index < span[1];
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
	
	private static void readAnnotation(String annotaitonFileName) {
		
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus();
		
		ExperimentUtils.loadSRLAnnotationSentences(trainCorpus);
	
		//annotateSentence((SRLSentence) trainCorpus.sentences.get(0));
	}
}