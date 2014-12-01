package scorer;

import util.LatticeUtils;
import data.DepCorpus;
import data.DepSentence;

/**
 * Use a a set of universal grammar rules to vote for edges.
 * @author luheng
 *
 */
public class UniversalGrammarScorer {
	// Set of Universal Grammar
	final String[][] universalGrammarRules = {
			{"ROOT", "VERB"},
			{"VERB", "VERB"},
			{"VERB", "NOUN"},
			{"VERB", "ADP"},
			{"VERB", "ADJ"},
			{"VERB", "ADV"},
			{"VERB", "PRON"},
			{"VERB", "PRT"},
			{"VERB", "X"},
			{"VERB", "."},
			{"NOUN", "NOUN"},
			{"NOUN", "ADJ"},
			{"NOUN", "ADP"},
			{"NOUN", "DET"},
			{"ADP", "NOUN"},
			
	};
	double[][] universalGrammarWeights;
	
	public UniversalGrammarScorer(DepCorpus corpus) {
		// 0 is reserved for ROOT.
		int numPostags = corpus.posDict.size() + 1;
		universalGrammarWeights = new double[numPostags][numPostags];
		LatticeUtils.fill(universalGrammarWeights, 0.0);
		for (String[] ugPair : universalGrammarRules) {
			int parent = ugPair[0].equals("ROOT") ? 0 :
				corpus.posDict.lookupString(ugPair[0]) + 1;
			int child = corpus.posDict.lookupString(ugPair[1]) + 1;
			assert (parent >= 0 && child >= 0);
			// We could assign different weights to each rules later.
			universalGrammarWeights[parent][child] += 1.0;
		}
	}
	
	public void getScores(double[][] scores, DepSentence sentence) {
		assert (sentence.length + 1 == scores.length);
		int length = sentence.length;
		LatticeUtils.fill(scores, 0.0);
		for (int i = 1; i <= length; i++) {
			// Compute root score.
			int childPos = sentence.postags[i-1] + 1;
			scores[0][i] += universalGrammarWeights[0][childPos];
			for (int j = 1; j <= length; j++) {
				int parentPos = sentence.postags[j-1] + 1;
				scores[j][i] += universalGrammarWeights[parentPos][childPos];
			}
		}
	}
	
}
