package learning;

import java.util.HashSet;

import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QuestionGenFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	private UniversalPostagMap univDict = null;
	public CountDictionary featureDict = null;
	//public final int numBestParses
	public final int minFeatureFreq;
	
	public boolean useLexicalFeatures = true;
	public boolean useDependencyFeatures = true;
	
	@SuppressWarnings("unused")
	private static final int WHSlotId = 0;
	private static final int AUXSlotId = 1;
	private static final int PH1SlotId = 2;
	private static final int TRGSlotId = 3;
	private static final int PH2SlotId = 4;
	private static final int PPSlotId = 5;
	private static final int PH3SlotId = 6;
	
	public QuestionGenFeatureExtractor(Corpus corpus, int minFeatureFreq) {
		this.corpus = corpus;
		this.minFeatureFreq = minFeatureFreq;
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
		univDict = ExperimentUtils.loadPostagMap();
		
		featureDict = new CountDictionary();
	}
	
	/**
	 * 0: WH
	 * 1: AUX
	 * 2: PH1
	 * 3: TRG
	 * 4: PH2
	 * 5: PP
	 * 6: PH3
	 */
	
	private static HashSet<String> getWhFeatures(
			Sentence sentence, int propHead, String wh) {
		HashSet<String> feats = new HashSet<String>();
		feats.add("WH=" + wh);
		return feats;
	}
	
	private static HashSet<String> getAuxFeatures(
			Sentence sentence, int propHead, String aux) {
		HashSet<String> feats = new HashSet<String>();
		if (aux.isEmpty()) {
			feats.add("AUX=null");
		} else {
			feats.add("AUX=" + aux);
		}
		if (aux.contains("not") || aux.contains("n\'t")) {
			feats.add("AUX_neg");
		}
		if (aux.contains("might") || aux.contains("would") ||
			aux.contains("could")) {
			feats.add("AUX_maybe");
		}
		return feats;
	}
	
	private static HashSet<String> getPhFeatures(
			Sentence sentence, int propHead, String ph, String prefix) {
		HashSet<String> feats = new HashSet<String>();
		if (ph.isEmpty()) {
			feats.add(prefix + "=null");
		} else {
			feats.add(prefix + "=" + ph);
		}
		// TODO: add ph3 options
		return feats;
	}
	
	private static HashSet<String> getPPFeatures(
			Sentence sentence, int propHead, String pp) {
		HashSet<String> feats = new HashSet<String>();
		if (pp.isEmpty()) {
			feats.add("PP=null");
		} else {
			feats.add("PP=" + pp);
		}
		// TODO: add more pp features
		return feats;
	}
	
	private static HashSet<String> getTrgFeatures(
			Sentence sentence, int propHead, String trg) {
		HashSet<String> feats = new HashSet<String>();
		feats.add("TRG=" + trg);
		// TODO: add more trg features
		return feats;
	}
	
	private static HashSet<String> getUnaryFeatures(
			Sentence sentence, int propHead, int slotId, String opt) {
		switch (slotId) {
		case 0: return getWhFeatures(sentence, propHead, opt);
		case 1: return getAuxFeatures(sentence, propHead, opt);
		case 2: return getPhFeatures(sentence, propHead, opt, "PH1");
		case 3: return getTrgFeatures(sentence, propHead, opt);
		case 4: return getPhFeatures(sentence, propHead, opt, "PH2");
		case 5: return getPPFeatures(sentence, propHead, opt);
		case 6: return getPhFeatures(sentence, propHead, opt, "PH3");
		default:
			return new HashSet<String>();
		}
	}
	
	private static void makeConjunctionFeatures(
			HashSet<String> unaryFeats, HashSet<String> conjFeats) {
		HashSet<String> tempFeats = new HashSet<String>();
		tempFeats.addAll(conjFeats);
		for (String uf : unaryFeats) {
			conjFeats.add(uf);
			for (String cf : tempFeats) {
				conjFeats.add(uf + "_" + cf);
			}
		}
	}
	
	public TIntDoubleHashMap extractFeatures(
			Sentence sentence, int propHead, String[][] lattice,
			int slotId, int[] latticeIds, boolean acceptNew) {		
		HashSet<String> conjFeats = new HashSet<String>();
		// TODO: add more specified conjunction features. i.e. voice(aux+trg)
		for (int i = 0; i < 3; i++) {
			int currSlotId = slotId - i;
			int currOptId = latticeIds[2 - i];
			// Create conjunction features.
			if (currSlotId >= 0) {
				HashSet<String> unaryFeats = getUnaryFeatures(
						sentence, propHead,
						currSlotId, lattice[currSlotId][currOptId]);
				// Make feature conjunctions
				makeConjunctionFeatures(unaryFeats, conjFeats);
			}
		}
		if (slotId == TRGSlotId) {
			// TODO...
		}
		
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		for (String feat : conjFeats) {
			fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
		}
		
		// TODO: add bias term.
		fv.remove(-1);
		// Binarize features.
		for (int fid : fv.keys()) {
			fv.put(fid, 1);
		}
		return fv;
	}

	public void freeze() {
		featureDict = new CountDictionary(featureDict, minFeatureFreq);
	}
}
