package annotation;

import data.DepSentence;
import data.QAPair;

public interface AbstractQuestionAnswerAligner {
	public void align(DepSentence sentence, QAPair qa);
}
