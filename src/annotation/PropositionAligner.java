package annotation;

import data.SRLSentence;
import data.Proposition;

public class PropositionAligner {
	
	public Proposition align(SRLSentence sentence, String propString) {
		return align(sentence, propString, 0);
	}
	
	/**
	 * We do this greedily - simply find the first alignment that gets the
	 * entire proposition.
	 * @param sentence
	 * @param propString
	 * @param start
	 * @return
	 */
	public Proposition align(SRLSentence sentence, String propString,
			int start) {
		Proposition aligned = new Proposition();
		String[] propTokens = propString.split("\\s+");
		for (int i = 0; i < propTokens.length; i++) {
			propTokens[i] = propTokens[i].trim();
		}
		aligned.sentence = sentence;
		for (int i = start; i < sentence.length; i++) {
			boolean allMatch = true;
			for (int j = 0; j < propTokens.length && i + j < sentence.length;
					j++) {
				if (!sentence.getTokenString(i + j)
						.equalsIgnoreCase(propTokens[j])) {
					allMatch = false;
					break;
				}
			}
			if (allMatch) {
				aligned.setPropositionSpan(i, i + propTokens.length);
				return aligned;
			}
		}
		System.out.println("Error: Unaligned proposition.");
		return null;
	}
}
