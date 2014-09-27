package annotation;

import data.DepSentence;
import data.QAAnnotation;

public interface AbstractQAAligner {
	public QAAnnotation alignQuestionAnswer(String question, String answer,
			                                DepSentence sentence);
}
