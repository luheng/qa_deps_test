package evaluation;

import data.SRLSentence;

public class DependencySRLEvaluation {

	public static void getAccuracy(SRLSentence sentence,
								   String[][] prediction) {
		String[][] semanticArcs = sentence.getSemanticArcs();
		
		// Return precision, recall, F1
	}
}
