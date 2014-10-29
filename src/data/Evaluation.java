package data;

public class Evaluation {
	
	public static double getAccuracy(DepSentence sentence, int[] prediction) {
		if (sentence.length != prediction.length) {
			System.out.println("Error: Sentence length does not match!");
			return 0.0;
		}
		double acc = 0.0;
		int accCounter = 0;
		for (int i = 0; i < sentence.length; i++) {
			// Ignore punctuation.
			if (sentence.getDeptagString(i).equals(".")) {
				continue;
			}
			accCounter += 1;
			if (sentence.parents[i] == prediction[i]) {
				acc += 1.0;
			}
		}
		if (accCounter == 0) {
			System.out.println("Error: Sentence does not contain any " +
							   "non-punctuation token.");
		}
		return acc / accCounter;
	}
}
