package constraints;

import data.DepSentence;
import data.QAPair;

/*
 * Check if a dependency tree violates the constraints with respect to a
 * question-answer pair.
 */
public class ConstraintChecker {
	public static boolean check(DepSentence sentence, QAPair qa, int[] tree) {
		AbstractConstraint
			answerConstraint1 = new AnswerIsSubtreeConstraint(),
			answerConstraint2 = new AnswerIsHeadlessSubtreeConstraint(),							
			qaConstraint = new SingleEdgeQAConstraint();
		return (answerConstraint1.validate(sentence, qa) ||
			   answerConstraint2.validate(sentence, qa)) &&
			   qaConstraint.validate(sentence, qa);
	}
}
;