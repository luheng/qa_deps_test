package constraints;

import data.DepSentence;
import data.QAPair;

public class NonEmptyQuestionConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		int numAligned = 0;
		for (int i : qa.questionAlignment) {
			if (i != -1) {
				numAligned += 1;
			}
		}
		return (numAligned > 0);
	}

}
