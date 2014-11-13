package postprocess;

import java.util.ArrayList;
import java.util.HashSet;

import util.LatticeUtils;
import data.DepCorpus;
import data.DepSentence;

/**
 * List of English auxliary verbs are taken from:
 * 		http://en.wikipedia.org/wiki/Auxiliary_verb
 * 
 *  In a parsed sentence, if the auxiliary verb is not the only verb, the head
 *  of the parse tree is assigned to another verb (the closest one), and all its
 *  children are also moved under the new head.
 *  
 *  Handle these two common cases:
 *  	1. "will do ...", where the main verb follows the auxiliary verb
 *  		immediately.
 *  	2. "will be doing", where the main verb follows two consequtive
 *  		auxiliary verbs. 
 *   
 * @author luheng
 *
 */
public class AuxiliaryVerbPostprocessor {
	private final String[] enAuxiliaryVerbs = {
			"be",
			"can",
			"could",
			"dare",
			"do",
			"have",
			"may",
			"might",
			"must",
			"need",
			"ought",
			"shall",
			"should",
			"will",
			"would"
	};
	
	private int verbPosId;
	private HashSet<Integer> enAuxiliaryVerbSet;
	
	public AuxiliaryVerbPostprocessor(DepCorpus corpus) {
		enAuxiliaryVerbSet = new HashSet<Integer>();
		for (String verb : enAuxiliaryVerbs) {
			int verbID = corpus.wordDict.lookupString(verb);
			if (verbID != -1) {
				enAuxiliaryVerbSet.add(verbID);
			}
		}
		verbPosId = corpus.posDict.lookupString("VERB"); 
	}
	
	public void postprocess(int[] newParents, int[] parents,
							DepSentence sentence) {
		LatticeUtils.copy(newParents, parents);
		
		for (int i = 0; i < sentence.length; i++) {
			if (isAuxiliaryVerb(sentence, i)) {
				if (isAuxiliaryVerb(sentence, i + 1) &&
					isVerb(sentence, i + 2)) {
					// change parent
					switchHead(newParents, i, i + 1, i + 2);
					/*
					System.out.println(String.format("%s(%d) %s(%d) %s(%d)",
									   sentence.getTokenString(i), i,
									   sentence.getTokenString(i+1), i + 1,
									   sentence.getTokenString(i + 2), i + 2));
					System.out.println("gold:\t" + sentence.parents[i] + ", " +
									   sentence.parents[i+1] + ", " +
									   sentence.parents[i+2]);
					System.out.println("pred:\t" + parents[i] + ", " +
							   			parents[i+1] + ", " +
							   			parents[i+2]);
					System.out.println("fixed:\t" + newParents[i] + ", " +
				   						newParents[i+1] + ", " +
				   						newParents[i+2]);
				   	*/
					i += 2;
				} else if (isVerb(sentence, i + 1)) {
					// change parent
					switchHead(newParents, i, i + 1);
					/*
					System.out.println(sentence.toString());
					System.out.println(String.format("%s(%d) %s(%d)",
							   		   sentence.getTokenString(i), i,
							   		   sentence.getTokenString(i+1), i + 1));
					System.out.println("gold:\t" + sentence.parents[i] + ", " +
							   		   sentence.parents[i+1]);
					System.out.println("pred:\t" + parents[i] + ", " +
							   		   parents[i+1]);
					System.out.println("fixed:\t" + newParents[i] + ", " +
					   		   			newParents[i+1]);
					*/
					i ++;
				}
				
			}
		}
		
	}
	
	private boolean isVerb(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == verbPosId;
	}
	private boolean isAuxiliaryVerb(DepSentence sentence, int id) {
		return //isVerb(sentence, id) &&
			   id < sentence.length &&
			   enAuxiliaryVerbSet.contains(sentence.tokens[id]);
	}
	
	private void switchHead(int[] parents, int auxVerb1, int auxVerb2,
							int mainVerb) {
		if (parents[auxVerb1] == mainVerb && parents[auxVerb2] == mainVerb) {
			// Nothing needs to be fixed.
			return;
		}
		if (parents[auxVerb1] != mainVerb) {
			parents[mainVerb] = parents[auxVerb1];
		} else {
			parents[mainVerb] = parents[auxVerb2];
		}
		parents[auxVerb1] = mainVerb;
		parents[auxVerb2] = mainVerb;
		for (int i = 0; i < parents.length; i++) {
			if (i == mainVerb) {
				continue;
			}
			if (parents[i] == auxVerb1 || parents[i] == auxVerb2) {
				parents[i] = mainVerb;
			}
		}
	}
	
	private void switchHead(int[] parents, int auxVerb, int mainVerb) {
		if (parents[auxVerb] == mainVerb) {
			// Nothing needs to be fixed.
			return;
		}
		parents[mainVerb] = parents[auxVerb];
		parents[auxVerb] = mainVerb;
		for (int i = 0; i < parents.length; i++) {
			if (i != mainVerb && parents[i] == auxVerb) {
				parents[i] = mainVerb;
			}
		}
	}
}
