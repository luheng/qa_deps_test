package constraints;

import java.util.Arrays;

import data.DepSentence;
import data.QAPair;

/* If in the answer, exists a word that is the head of the question.
 * 
 */

public class ReversedEdgeQAConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa, int[] tree) {
		int[] inverseAlignment = new int[sentence.length];
		Arrays.fill(inverseAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int aligned = qa.answerAlignment[i];
			if (aligned != -1) {
				inverseAlignment[aligned] = i;
			}
		}
		int numReversedEdges = 0;
		for (int i = 0; i < qa.questionAlignment.length; i++) {
			int aligned = qa.questionAlignment[i];
			if (aligned != -1) {
				int parent = tree[i];
				if (parent != -1 && inverseAlignment[parent] != -1) {
					numReversedEdges ++;
				}
			}
		}
		return (numReversedEdges > 0);
	}
	
	@Override
	public String toString() {
		return "Exists edge from answer to question";
	}
}
