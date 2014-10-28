package constraints;

import java.util.Arrays;

import util.StringUtils;
import data.DepSentence;
import data.QAPair;

/*
 * Assume there is exactly one edge going from question to answer.
 */
public class SingleEdgeQAConstraint implements AbstractConstraint {

	private int getSubtreeHead(DepSentence sentence, QAPair qa) {
		int[] inverseAlignment = new int[sentence.length];
		Arrays.fill(inverseAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			if (qa.answerAlignment[i] != -1) {
				inverseAlignment[qa.answerAlignment[i]] = i;
			}
		}
		int numOutGoingEdges = 0, headInside = -1, headOutside = -1;
		for (int i = 0; i < sentence.length; i++) {
			int aligned = inverseAlignment[i];
			if (aligned != -1) {
				int parent = sentence.parents[i];
				if (parent == -1) {
					numOutGoingEdges += 1;
					headInside = i;
				} else if (inverseAlignment[parent] == -1) {
					numOutGoingEdges += 1;
					headInside = i;
					if (headOutside < 0) {
						headOutside = parent;
					} else if (headOutside != parent) {
						return -1;
					}
				}
			}
		}
		if (numOutGoingEdges == 1) {
		//	System.out.println(StringUtils.join(" ", qa.answerTokens) +
		//			"- headInside: " + headInside);
			return headInside;
		} else {
		//	System.out.println(StringUtils.join(" ", qa.answerTokens) +
		//			"- headOutside: " + headOutside);
			return headOutside;
		}
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		int subtreeHead = getSubtreeHead(sentence, qa);
		if (subtreeHead < 0) {
			// Answer does not represent a single subtree.
			return false;			
		}
		// System.out.println("subtree head:\t" + subtreeHead);
		for (int i : qa.questionAlignment) {
			// Here we allow question to contain the answer 's head.
			if (i == sentence.parents[subtreeHead] || i == subtreeHead) {
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
