package constraints;

import data.DepSentence;
import data.QAPairOld;

public interface AbstractConstraint {	
	public boolean validate(DepSentence sentence, QAPairOld qa);
	
	public boolean validate(DepSentence sentence, QAPairOld qa, int[] tree);
}
