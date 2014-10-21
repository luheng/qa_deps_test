package constraints;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

/*
 * There might be several outgoing edges from the answer span. But all the
 * out going edges leads to the same word.
 */
public class AnswerIsHeadlessSubtreeConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		int[] inversedAlignment = new int[sentence.length];
		Arrays.fill(inversedAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inversedAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0, lastParent = -1;
		for (int i = 0; i < sentence.length; i++) {
			if (inversedAlignment[i] != -1) {
				int parent = sentence.parents[i];
				if (parent == -1) {
					return false;
				}
				if (inversedAlignment[parent] == -1) {
					numOutGoingEdges += 1;
					if (lastParent < 0) {
						lastParent = parent;
					} else if (lastParent != parent) {
						return false;
					}
				}
			}
		}
		return (numOutGoingEdges > 1);
	}

	@Override
	public String toString() {
		return "Answer is a headless subtree";
	}
}
