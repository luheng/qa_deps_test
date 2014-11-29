package constraints;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

/*
 * A more "strict" constraint than the SingleEdgeQAConstraint.
 * This doesn't allow the answer span to have an "outside" head. 
 */
public class StrictSingleEdgeQAConstraint implements AbstractConstraint {

	private int getSubtreeHead(DepSentence sentence, QAPair qa, int[] tree) {
		int[] inverseAlignment = new int[sentence.length];
		Arrays.fill(inverseAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inverseAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0, answerHead = -1;
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int aligned = qa.answerAlignment[i];
			if (aligned == -1) {
				continue;
			}
			int parent = tree[aligned];
			// Parent needs to be in answer span.
			if (parent == -1 || inverseAlignment[parent] == -1) {
				numOutGoingEdges += 1;
				answerHead = aligned;
			}
		}
		/*
		System.out.println(qa.toString() + 
						   "\t#out-going edges:\t" + numOutGoingEdges +
						   "\tanswer-head:\t" + answerHead);
		*/
		if (numOutGoingEdges == 1) {
			return answerHead;
		}
		return -1;
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa, int[] tree) {
		int subtreeHead = getSubtreeHead(sentence, qa, tree);
		if (subtreeHead < 0) {
			// Answer does not represent a single subtree.
			return false;			
		}
		for (int i : qa.questionAlignment) {
			// Here we allow question to contain the answer 's head.
			if (i == tree[subtreeHead] || i == subtreeHead) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Single edge from question to answer";
	}

}
