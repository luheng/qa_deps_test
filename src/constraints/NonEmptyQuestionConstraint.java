package constraints;

import data.DepSentence;
import data.QAPairOld;

public class NonEmptyQuestionConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa, int[] tree) {
		int numAligned = 0;
		for (int i : qa.questionAlignment) {
			if (i != -1) {
				numAligned += 1;
			}
		}
		return (numAligned > 0);
	}

	@Override
	public String toString() {
		return "Question has non-empty alignment";
	}
}
