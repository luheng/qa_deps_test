package annotation;

import data.DepSentence;
import data.QAPairOld;

public interface AbstractQuestionAnswerAligner {
	public void align(DepSentence sentence, QAPairOld qa);
}
