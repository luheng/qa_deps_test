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
		int[] inversedAlignment = new int[sentence.length];
		Arrays.fill(inversedAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inversedAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0;
		for (int i = 0; i < sentence.length; i++) {
			if (inversedAlignment[i] != -1) {
				int parent = sentence.parents[i];
				if (parent == -1 || inversedAlignment[parent] == -1) {
					numOutGoingEdges += 1;
				}
			}
		}
		return numOutGoingEdges == 1;
	}

	@Override
	public String toString() {
		return "Answer is a subtree";
	}
}
