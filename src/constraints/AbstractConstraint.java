package constraints;

import data.DepSentence;
import data.QAPair;

public interface AbstractConstraint {	
	public boolean validate(DepSentence sentence, QAPair qa);
}
