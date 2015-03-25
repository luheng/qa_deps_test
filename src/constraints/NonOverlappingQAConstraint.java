package constraints;

import data.DepSentence;
import data.QAPairOld;

public class NonOverlappingQAConstraint implements AbstractConstraint {

	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa) {
		
		return false;
	}
	
	@Override
	public boolean validate(DepSentence sentence, QAPairOld qa, int[] tree) {
		
		return false;
	}

}
