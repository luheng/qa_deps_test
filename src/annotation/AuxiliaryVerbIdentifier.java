package annotation;

import java.util.Arrays;
import java.util.HashSet;

import util.StrUtils;
import data.DepCorpus;
import data.DepSentence;
import data.Sentence;

public class AuxiliaryVerbIdentifier {
	private static final String[] enAuxiliaryVerbs = {
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
			"having",
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
			"ought",
		//	"ought to",
		//	"is going to"
	};
	
	private HashSet<String> copulaVerbs;
	
	private int verbPosID, advPosID;
	private static HashSet<String> enAuxiliaryVerbSet;
	
	public AuxiliaryVerbIdentifier(DepCorpus corpus) {
		verbPosID = corpus.posDict.lookupString("VERB");
		advPosID = corpus.posDict.lookupString("ADV");
		copulaVerbs = StrUtils.asSet("be", "being", "am", "\'m", "is",
				"\'s", "are", "\'re", "was", "were", "been", "being");
	}
	
	public boolean ignoreVerbForSRL(DepSentence sentence, int idx) {
		if (!isAuxiliaryVerb(sentence, idx)) {
			return false;
		}
		String token = sentence.getTokenString(idx).toLowerCase();
		int length = sentence.length;
		// "have to"
		if ((token.equals("have") || token.equals("had") ||
			 token.equals("has")) && idx < length - 1 &&
			 sentence.getTokenString(idx+1).equals("to")) {
			return false;
		}
		if (isVerb(sentence, idx + 1) || isVerb(sentence, idx + 2) ||
			(isNegationWord(sentence, idx + 1) && isVerb(sentence, idx + 3))) {
			return true;
		}
		// "Stand alone auxiliary verbs"
		return copulaVerbs.contains(token);
	}
	
	/**
	 * Identify auxiliary verb groups by simple pattern matching.
	 * @param sentence
	 */
	public void postprocess(DepSentence sentence, int[] verbHeads) {
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
						//   isModifierWord(sentence, i + 2) &&
						   isVerb(sentence, i + 3)) {
					// e.g. has n't been doing
					//auxVerbs.add(new Span(i, i + 4));
					verbHeads[i] = verbHeads[i+1] = verbHeads[i+2] = i + 3;
					i += 3;
				} else if (//isModifierWord(sentence, i + 1) &&
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
	
	public static boolean isAuxiliaryVerb(Sentence sentence, int id) {
		if (enAuxiliaryVerbSet == null) {
			enAuxiliaryVerbSet = new HashSet<String>();
			for (String verb : enAuxiliaryVerbs) {
				enAuxiliaryVerbSet.add(verb);
			}
		}
		return id < sentence.length && enAuxiliaryVerbSet.contains(
				   sentence.getTokenString(id).toLowerCase());
	}
	
	public boolean isNegationWord(Sentence sentence, int id) {
		if(id < sentence.length) {
			String tok = sentence.getTokenString(id);
			return tok.equalsIgnoreCase("n\'t") || tok.equalsIgnoreCase("not");
		}
		return false;
	}
	
	public boolean isModifierWord(DepSentence sentence, int id) {
		return id < sentence.length && sentence.postags[id] == advPosID; 
	}
}
