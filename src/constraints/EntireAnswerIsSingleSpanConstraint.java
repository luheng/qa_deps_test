package constraints;

import data.DepSentence;
import data.QAPairOld;

public class EntireAnswerIsSingleSpanConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa, int[] tree) {
		int spanStart = qa.answerAlignment[0];
		if (spanStart == -1) {
			return false;
		}
		for (int i = 1; i < qa.answerAlignment.length - 1; i++) {
			if (qa.answerAlignment[i] != spanStart + i) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "Entire answer is single span";
	}
}
