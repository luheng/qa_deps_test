package constraints;

import data.DepSentence;
import data.QAPair;

/*
 * Assume there is exactly one edge going from question to answer.
 */
public class SingleEdgeQAConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		// TODO Auto-generated method stub
		return false;
	}

}
