package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import annotation.AuxiliaryVerbIdentifier;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.VerbInflectionDictionary;
import data.WiktionaryPosDictionary;
import evaluation.F1Metric;

public class WiktionaryVerbIdentifier {

	private static final String wiktInflPath =
			"wiktionary/en_verb_inflections.txt";
	private static final String wiktPosPath =
			"wiktionary/en_postags_withverb.txt";
	
	private VerbInflectionDictionary verbDict = null;
	private WiktionaryPosDictionary wikposDict = null;
	private AuxiliaryVerbIdentifier auxDict = null;
	
	public WiktionaryVerbIdentifier(SRLCorpus corpus) {
		verbDict = new VerbInflectionDictionary(corpus);
		auxDict = new AuxiliaryVerbIdentifier(corpus);
		wikposDict = new WiktionaryPosDictionary(corpus);
		
		try {
			verbDict.loadDictionaryFromFile(wiktInflPath);
			wikposDict.loadDictionaryFromFile(wiktPosPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> extractContentVerbs(DepSentence sentence) {
		int length = sentence.length;
		boolean[] isVerb = new boolean[length];
		boolean[] isAux = new boolean[length];
		Arrays.fill(isVerb, false);
		Arrays.fill(isAux, false);
		for (int i = 0; i < length; i++) {
			String token = sentence.getTokenString(i).toLowerCase();
			if (verbDict.inflMap.containsKey(token)) {
				// 1. Verbs should not be upper-cased.
				if (i > 0 && !token.equals(sentence.getTokenString(i))) {
					continue;
				}
				for (int infl : verbDict.inflMap.get(token)) {
					String verb = verbDict.inflections.get(infl)[0];
					int[] posIds = wikposDict.posMap.get(verb);
					String bestPos = wikposDict.posDict.getString(posIds[0]);
					if (bestPos.equals("verb") || bestPos.equals("noun") ||
						bestPos.equals("adjective")) {
						isVerb[i] = true;
						break;
					}
				}
					
				//isVerb[i] = true;
			}
			// This function, specifically, does not use pos-tag information.
			if (auxDict.isAuxiliaryVerb(sentence,i)) {
				isAux[i] = true;
			}
		}
		ArrayList<Integer> verbs = new ArrayList<Integer>();
		for (int i = 0; i < length; i++) {
			if (isVerb[i] && !isAux[i]) {
				verbs.add(i);
			}
		}
		return verbs;
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
	
		WiktionaryVerbIdentifier verbId =
				new WiktionaryVerbIdentifier(trainCorpus);
		
		F1Metric avgAcc = new F1Metric();
		//for (int i = 0; i < trainCorpus.sentences.size(); i++) {
		for (int i = 0; i < 100; i++) {
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(i);
			ArrayList<Integer> verbs = verbId.extractContentVerbs(sentence);
			
			// Validate against gold propositions.
			boolean[] matched = new boolean[verbs.size()];
			Arrays.fill(matched, false);
			
			int numMatches = 0, numGold = 0;
			for (Proposition prop : sentence.propositions) {
				if (!sentence.getPostagString(prop.propID).equals("VERB")) {
					continue;
				}
				System.out.println(prop);
				++ numGold;
				for (int j = 0; j < verbs.size(); j++) {
					if (prop.propID == verbs.get(j)) {
						numMatches ++;
						matched[j] = true;
					}
				}
			}
			if (numGold == 0) {
				continue;
			}
			F1Metric f1 = new F1Metric(numMatches, numGold, verbs.size());
			//System.out.println(f1);
			if (f1.precision() < 1) {
				System.out.println(sentence.getTokensString());
				for (int j = 0; j < verbs.size(); j++) {
					if (!matched[j]) {
						System.out.println(sentence.getTokenString(verbs.get(j)));
					}
				}
				System.out.println();
			}
			avgAcc.add(f1);
		}
		System.out.println("Averaged:\t" + avgAcc);
	}
}
