package postprocess;

import util.LatticeUtils;
import data.DepCorpus;
import data.DepSentence;

/**
 * Change a chain that starts with a NOUN to a "claw" like shape.
 * @author luheng
 *
 */
public class FlatNounPhrasePostprocessor implements AbstractPostprocessor {

	private int adjID, detID, nounID;
	
	public FlatNounPhrasePostprocessor(DepCorpus corpus) {
		adjID = corpus.posDict.lookupString("ADJ");
		detID = corpus.posDict.lookupString("DET");
		nounID = corpus.posDict.lookupString("NOUN");
	}
	
	@Override
	public void postprocess(int[] newParents, int[] parents,
			DepSentence sentence) {
		LatticeUtils.copy(newParents, parents);
		// Lookup NP chain from end to beginning of the sentence.
		for (int i = sentence.length - 1; i >= 0; i--) {
			if (sentence.postags[i] == nounID) {
				int j = i; 
				for ( ; j >= 0; j--) {
					int pos = sentence.postags[j];
					if (pos != adjID && pos != detID && pos != nounID) {
						break;
					}
					if (parents[j] < j && parents[j] > i) {
						break;
					}
				}
				if (i - j > 1) {
					// Fix the chain.
					/*
					for (int k = j + 1; k <= i; k++) {
						System.out.print(sentence.getTokenString(k) + " ");
					}
					System.out.println();
					*/
					for (int k = i - 1; k > j; k--) {
						newParents[k] = i;
					}
					i = j + 1;
				} 
			}
		}
	}

}
