package constraints;

import data.DepSentence;
import data.QAPair;

/*
 * Assume the answer is a single span.
 */
public class AnswerIsSingleSpanConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		return validate(sentence, qa, sentence.parents);
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPair qa, int[] tree) {
		int spanStart = -1;
		int numSpans = 0;
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int alignment = qa.answerAlignment[i];
			if (alignment == -1) {
				if (spanStart != -1) {
					spanStart = -1;
					numSpans += 1;
				}
				continue;
			}
			if (spanStart == -1) {
				if (numSpans > 0) {
					return false;
				}
				spanStart = i;
				continue;
			}
			if (alignment - qa.answerAlignment[spanStart] != i - spanStart) {
				return false;
			}
		}
		return (numSpans > 0 || spanStart >= 0);
	}

	@Override
	public String toString() {
		return "Answer is a single span";
	}
}
