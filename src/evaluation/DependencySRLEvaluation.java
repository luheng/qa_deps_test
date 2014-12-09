package evaluation;

import data.SRLSentence;

public class DependencySRLEvaluation {

	public static F1Metric getUnlabeledAccuracy(SRLSentence sentence,
							    				String[][] prediction) {
		String[][] gold = sentence.getSemanticArcs();
		F1Metric accuracy = new F1Metric();
		for (int i = 0; i < gold.length; i++) {
			for (int j = 0; j < gold[i].length; j++) {
				if (!gold[i][j].isEmpty()) {
					accuracy.numGold ++;
					if (!prediction[i][j].isEmpty()) {
						accuracy.numMatched ++;
					}
				}
				if (!prediction[i][j].isEmpty()) {
					accuracy.numProposed ++;
				}
			}
		}
		return accuracy;
	}
	
	public static F1Metric getLabeledAccuracy(SRLSentence sentence,
							    			  String[][] prediction) {
		String[][] gold = sentence.getSemanticArcs();
		F1Metric accuracy = new F1Metric();
		for (int i = 0; i < gold.length; i++) {
			for (int j = 0; j < gold[i].length; j++) {
				if (!gold[i][j].isEmpty()) {
					accuracy.numGold ++;
					if (gold[i][j].equals(prediction[i][j])) {
						accuracy.numMatched ++;
					}
				}
				if (!prediction[i][j].isEmpty()) {
					accuracy.numProposed ++;
				}
			}
		}
		return accuracy;
	}
}
