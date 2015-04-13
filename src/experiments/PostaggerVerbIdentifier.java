package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import annotation.AuxiliaryVerbIdentifier;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.UniversalPostagMap;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import evaluation.F1Metric;

public class PostaggerVerbIdentifier {	
	private MaxentTagger tagger = null;
	private AuxiliaryVerbIdentifier auxDict = null;
	private UniversalPostagMap univPos = null;
	
	public PostaggerVerbIdentifier(SRLCorpus corpus) {
		tagger = new MaxentTagger("libs/english-left3words-distsim.tagger");
		univPos = new UniversalPostagMap();
		try {
			univPos.loadFromFile("/Users/luheng/data/CONLL-x/univmap/en-ptb.map");
		} catch (IOException e) {
			e.printStackTrace();
		}
		auxDict = new AuxiliaryVerbIdentifier(corpus);
	}
	
	public ArrayList<Integer> extractContentVerbs(DepSentence sentence) {
		int length = sentence.length;
		boolean[] isVerb = new boolean[length];
		boolean[] isAux = new boolean[length];
		Arrays.fill(isVerb, false);
		Arrays.fill(isAux, false);
		String tagged = tagger.tagTokenizedString(sentence.getTokensString());
		String[] tags = tagged.split(" ");
	
		for (int i = 0; i < length; i++) {
			//String token = sentence.getTokenString(i);
			String tag = tags[i].split("_")[1];
			if (univPos.getUnivPostag(tag).equals("VERB")) {
				isVerb[i] = true;
			}
			// This function, specifically, does not use pos-tag information.
			if (auxDict.isAuxiliaryVerb(sentence,i)) {
				isAux[i] = true;
			}
		}
		ArrayList<Integer> verbs = new ArrayList<Integer>();
		for (int i = 0; i < length; i++) {
			// Simple auxverb test.
			if (isAux[i]) {
				String token = sentence.getTokenString(i).toLowerCase();
				if ((token.equals("have") || token.equals("had") || token.equals("has")) &&
					i < length - 1 && sentence.getTokenString(i+1).equals("to")) {
					verbs.add(i);
				}
				if ((i < length - 1 && isVerb[i+1]) ||
					(i < length - 2 && isVerb[i+2])) {
					continue;
				}
				if (token.equals("have") || token.equals("had") || token.equals("has") ||
					token.equals("do") || token.equals("did") || token.equals("done") ||
					token.equals("does") || token.equals("need")) {
					verbs.add(i);
				}
			}
			if (isVerb[i] && !isAux[i]) {
				verbs.add(i);
			}
		}
		return verbs;
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
	
		PostaggerVerbIdentifier verbId =
				new PostaggerVerbIdentifier(trainCorpus);
		
		F1Metric avgAcc = new F1Metric();
		for (int i = 0; i < trainCorpus.sentences.size(); i++) {
		//for (int i = 0; i < 100; i++) {
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(i);
			ArrayList<Integer> verbs =
					verbId.extractContentVerbs(trainCorpus.getSentence(i));
			
			
			// Validate against gold propositions.
			boolean[] matched = new boolean[verbs.size()];
			Arrays.fill(matched, false);
			
			ArrayList<Integer> uncoveredGold = new ArrayList<Integer>();
			int numMatches = 0, numGold = 0;
			for (Proposition prop : sentence.propositions) {
				if (!sentence.getPostagString(prop.propID).equals("VERB")) {
					continue;
				}
				//System.out.println(prop);
				++ numGold;
				boolean goldCovered = false;
				for (int j = 0; j < verbs.size(); j++) {
					if (prop.propID == verbs.get(j)) {
						numMatches ++;
						matched[j] = true;
						goldCovered = true;
						break;
					}
				}
				if (!goldCovered) {
					uncoveredGold.add(prop.propID);
				}
			}
			if (numGold == 0) {
				continue;
			}
			F1Metric f1 = new F1Metric(numMatches, numGold, verbs.size());
			//System.out.println(f1);
			
			avgAcc.add(f1);
			if (f1.recall() > 0.99) {
				continue;
			}
			System.out.println(sentence.getTokensString());
			for (int p : uncoveredGold) {
				System.out.println("Recall loss:\t" + sentence.getTokenString(p));
			}
			for (int j = 0; j < verbs.size(); j++) {
				if (!matched[j]) {
					System.out.println("Precision loss:\t" + sentence.getTokenString(verbs.get(j)));
				}
			}
			System.out.println();
			
		}
		System.out.println("Averaged:\t" + avgAcc);
	}
}
