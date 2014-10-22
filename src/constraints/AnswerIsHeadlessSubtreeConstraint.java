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
		int[] inverseAlignment = new int[sentence.length];
		Arrays.fill(inverseAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inverseAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0, subtreeHead = -1;
		for (int i = 0; i < sentence.length; i++) {
			if (inverseAlignment[i] != -1) {
				int parent = sentence.parents[i];
				if (parent == -1) {
					return false;
				}
				if (inverseAlignment[parent] == -1) {
					numOutGoingEdges += 1;
					if (subtreeHead < 0) {
						subtreeHead = parent;
						
					} else if (subtreeHead != parent) {
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
