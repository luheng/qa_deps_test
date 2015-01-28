package annotation;

import java.util.Arrays;
import java.util.HashSet;

import data.DepCorpus;
import data.DepSentence;

public class AuxiliaryVerbIdentifier {
	private final String[] enAuxiliaryVerbs = {
			"be",
			"being",
			"am",
			"\'m",
			"is",
			"\'s",
			"are",
			"\'re",
			"was",
			"were",
			"been",
			"will",
			"\'ll",
			"would",
			"\'d",
			"wo", /* this is part of wo n't ... */
			"do",
			"does",
			"did",
			"done",
			"have",
			"\'ve",
			"has",
			"had",
			"ca", /* in ca n't */
			"can",
			"could",
			"may",
			"might",
			"must",
			"need",
			"shall",
			"should",
		//	"ought to",
		//	"is going to"
	};
	
	private int verbPosID, advPosID;
	private HashSet<Integer> enAuxiliaryVerbSet;
	
	public AuxiliaryVerbIdentifier(DepCorpus corpus) {
		enAuxiliaryVerbSet = new HashSet<Integer>();
		for (String verb : enAuxiliaryVerbs) {
			int verbID = corpus.wordDict.lookupString(verb);
			if (verbID != -1) {
				enAuxiliaryVerbSet.add(verbID);
			}
		}
		verbPosID = corpus.posDict.lookupString("VERB");
		advPosID = corpus.posDict.lookupString("ADV");
	}
	
	/**
	 * Identify auxiliary verb groups by simple pattern matching.
	 * @param sentence
	 */
	public void process(DepSentence sentence, int[] verbHeads) {
		Arrays.fill(verbHeads, -1);
		// ArrayList<Span> auxVerbs = new ArrayList<Span>();
		for (int i = 0; i < sentence.length; i++) {
			if (isAuxiliaryVerb(sentence, i)) {
				if (isAuxiliaryVerb(sentence, i + 1) &&
					isVerb(sentence, i + 2)) {
					// e.g. has been doing
					//auxVerbs.add(new Span(i, i + 3));
					verbHeads[i] = verbHeads[i+1] = i + 2;
					i += 2;
				} else if (isAuxiliaryVerb(sentence, i + 1) &&
						   isModifierWord(sentence, i + 2) &&
						   isVerb(sentence, i + 3)) {
					// e.g. has n't been doing
					//auxVerbs.add(new Span(i, i + 4));
					verbHeads[i] = verbHeads[i+1] = verbHeads[i+2] = i + 3;
					i += 3;
				} else if (isModifierWord(sentence, i + 1) &&
						   isVerb(sentence, i + 2)) {
					// e.g. is hurriedly doing
					//auxVerbs.add(new Span(i, i + 3));
					verbHeads[i] = verbHeads[i+1] = i + 2;
					i += 2;
				} else if (isVerb(sentence, i + 1)) {
					// change parent
					//auxVerbs.add(new Span(i, i + 2));
					verbHeads[i] = i + 1;
					i ++;
				}
				
			}
		}
		//return auxVerbs;
	}
	
	public boolean isVerb(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == verbPosID;
	}
	
	public boolean isAuxiliaryVerb(DepSentence sentence, int id) {
		return id < sentence.length &&
			   enAuxiliaryVerbSet.contains(sentence.tokens[id]);
	}
	
	public boolean isModifierWord(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == advPosID; 
	}
}
