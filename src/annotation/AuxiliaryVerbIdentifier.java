package annotation;

import java.util.ArrayList;
import java.util.HashSet;

import data.DepCorpus;
import data.DepSentence;
import data.Span;

public class AuxiliaryVerbIdentifier {
	private final String[] enAuxiliaryVerbs = {
			"be",
			"is",
			"are",
			"was",
			"were",
			"been",
			"can",
			"could",
			"dare",
			"do",
			"does",
			"did",
			"done",
			"have",
			"has",
			"had",
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
	public ArrayList<Span> process(DepSentence sentence) {
		ArrayList<Span> auxVerbs = new ArrayList<Span>();
		for (int i = 0; i < sentence.length; i++) {
			if (isAuxiliaryVerb(sentence, i)) {
				if (isAuxiliaryVerb(sentence, i + 1) &&
					isVerb(sentence, i + 2)) {
					// e.g. has been doing
					auxVerbs.add(new Span(i, i + 3));
					i += 2;
				} else if (isAuxiliaryVerb(sentence, i + 1) &&
						   isModifierWord(sentence, i + 2) &&
						   isVerb(sentence, i + 3)) {
					// e.g. has n't been doing
					auxVerbs.add(new Span(i, i + 4));
					i += 3;
				} else if (isModifierWord(sentence, i + 1) &&
						   isVerb(sentence, i + 2)) {
					// e.g. is hurriedly doing
					auxVerbs.add(new Span(i, i + 3));
					i += 2;
				} else if (isVerb(sentence, i + 1)) {
					// change parent
					auxVerbs.add(new Span(i, i + 2));
					i ++;
				}
				
			}
		}
		return auxVerbs;
	}
	
	private boolean isVerb(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == verbPosID;
	}
	private boolean isAuxiliaryVerb(DepSentence sentence, int id) {
		return id < sentence.length &&
			   enAuxiliaryVerbSet.contains(sentence.tokens[id]);
	}
	
	private boolean isModifierWord(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == advPosID; 
	}
}
