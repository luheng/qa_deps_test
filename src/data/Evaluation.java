package data;

public class Evaluation {
	
	public static Accuracy getAccuracy(DepSentence sentence, int[] prediction) {
		if (sentence.length != prediction.length) {
			System.out.println("Error: Sentence length does not match!");
			return new Accuracy(0, 0);
		}
		int acc = 0, accCounter = 0;
		for (int i = 0; i < sentence.length; i++) {
			// Ignore punctuation.
			if (sentence.getPostagString(i).equals(".")) {
				continue;
			}
			accCounter += 1;
			if (sentence.parents[i] == prediction[i]) {
				acc += 1;
			}
		}
		if (accCounter == 0) {
			System.out.println("Error: Sentence does not contain any " +
							   "non-punctuation token.");
			return new Accuracy(0, 0);
		}
		return new Accuracy(acc, accCounter);
	}
}
