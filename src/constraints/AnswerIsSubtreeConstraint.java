package constraints;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

/*
 * Answer is part of the subtree, which means there is exactly one edge leading
 *   out from the span.
 * How to check: for each word in the answer span, its parent is also part of
 *   the answer, except for exactly one word.
 */
public class AnswerIsSubtreeConstraint implements AbstractConstraint {
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa, int[] tree) {
		int[] inverseAlignment = new int[sentence.length];
		Arrays.fill(inverseAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inverseAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0;
		for (int i = 0; i < sentence.length; i++) {
			if (inverseAlignment[i] != -1) {
				int parent = tree[i];
				if (parent == -1 || inverseAlignment[parent] == -1) {
					numOutGoingEdges += 1;
				}
			}
		}
		if (numOutGoingEdges > 1) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Answer is a subtree";
	}
}
