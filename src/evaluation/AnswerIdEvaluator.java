package evaluation;

import java.util.Arrays;

public class AnswerIdEvaluator {

	public static F1Metric evaluate(
			double[] predScores,
			int[] goldAnswerFlags,
			int[] goldAnswerHeads,
			AnswerIdEvaluationParameters eval) {
		
		int length = predScores.length;
		int[] binaryPred = new int[length];
		int[] kbestPred = new int[eval.kBest];
		Arrays.fill(binaryPred, 0);
		Arrays.fill(kbestPred, -1);
		
		for (int i = 0; i < length; i++) {
			if (predScores[i] < eval.threshold) {
				continue;
			}
			binaryPred[i] = 1;
			if (eval.evalBinary()) {
				continue;
			}
			for (int j = 0; j < eval.kBest; j++) {
				if (kbestPred[j] == -1) {
					kbestPred[j] = i;
					break;
				} else if (predScores[kbestPred[j]] < predScores[i]) {
					for (int k = eval.kBest - 1; k > j; k--) {
						kbestPred[k] = kbestPred[k - 1];
					}
					kbestPred[j] = i;
					break;
				}
			}
		}
		
		F1Metric result = new F1Metric();
		for (int i = 0; i < length; i++) {
			if (goldAnswerHeads[i] == 1) {
				result.numGold ++;
			}			
		}
		if (eval.evalBinary()) {
			for (int i = 0; i < length; i++) {
				if (binaryPred[i] == 1) {
					result.numProposed ++;
					if ((eval.evalHead() && goldAnswerHeads[i] > 0) ||
						(!eval.evalHead() && goldAnswerFlags[i] > 0)) {
						result.numMatched ++;
					}
				}
			}
		} else {
			for (int i : kbestPred) {
				if (i >= 0) {
					result.numProposed ++;
					if ((eval.evalHead() && goldAnswerHeads[i] > 0) ||
						(!eval.evalHead() && goldAnswerFlags[i] > 0)) {
							result.numMatched ++;
					}
				}
			}
		}
		if (!eval.evalMulti()) {
			result.numGold = Math.max(result.numGold, 1);
			result.numProposed = Math.max(result.numProposed, 1);
			result.numMatched = Math.max(result.numMatched, 1);
		}
		return result;
	}
}
