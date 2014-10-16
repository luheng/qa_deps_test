package annotation;

import data.DepSentence;
import data.QAPair;

public interface QuestionAnswerAligner {
	public void align(DepSentence sentence, QAPair qa);
}
