package constraints;

import data.DepSentence;
import data.QAPairOld;

/*
 * Check if a dependency tree violates the constraints with respect to a
 * question-answer pair.
 */
public class ConstraintChecker {
	public static boolean check(DepSentence sentence, QAPairOld qa, int[] tree) {
		AbstractConstraint
			answerConstraint1 = new AnswerIsSubtreeConstraint(),
			answerConstraint2 = new AnswerIsHeadlessSubtreeConstraint(),							
			//qaConstraint = new SingleEdgeQAConstraint();
			qaConstraint = new StrictSingleEdgeQAConstraint();
		return (answerConstraint1.validate(sentence, qa, tree) ||
			   answerConstraint2.validate(sentence, qa, tree)) &&
			   qaConstraint.validate(sentence, qa, tree);
	}
}
;