package constraints;

import data.DepSentence;
import data.QAPair;

public class NonOverlappingQAConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPair qa) {
		
		return false;
	}

}
