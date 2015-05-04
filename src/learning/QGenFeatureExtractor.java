package learning;

import java.util.HashSet;

import annotation.QASlotPrepositions;
import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QGenFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	private UniversalPostagMap univDict = null;
	public CountDictionary featureDict = null;
	//public final int numBestParses
	public final int minFeatureFreq;
	
	public boolean useLexicalFeatures = true;
	public boolean useDependencyFeatures = true;
	
	public QGenFeatureExtractor(Corpus corpus, int minFeatureFreq) {
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
	
	private static HashSet<String> getWhFeatures(String wh) {
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
	
	private static HashSet<String> getAuxFeatures(String aux) {
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
	
	private static HashSet<String> getPhFeatures(String ph, String prefix) {
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
		boolean ppInSentence = false, ppInCommon = false;
		for (int i = 0; i < sentence.length; i++) {
			if (sentence.getTokenString(i).equalsIgnoreCase(pp)) {
				ppInSentence = true;
				break;
			}
		}
		for (String mfpp : QASlotPrepositions.mostFrequentPPs) {
			if (mfpp.equals(pp)) {
				ppInCommon = true;
				break;
			}
		}
		if (pp.isEmpty()) {
			feats.add("PP=null");
		} else if (ppInSentence || ppInCommon) {
			feats.add("PP=" + pp);
		}
		// TODO: add more pp features
		return feats;
	}
	
	private static HashSet<String> getPPFeatures(String pp) {
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
	
	private static HashSet<String> getTrgFeatures(String trg) {
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
	
	private static HashSet<String> getTransitionFeatures(
			int slotId, String opt) {
		switch (slotId) {
		case 0: return getWhFeatures(opt);
		case 1: return getAuxFeatures(opt);
		case 2: return getPhFeatures(opt, "PH1");
		case 3: return getTrgFeatures(opt);
		case 4: return getPhFeatures(opt, "PH2");
		case 5: return getPPFeatures(opt);
		case 6: return getPhFeatures(opt, "PH3");
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
			int slotId, int s, int sp, int spp, boolean acceptNew) {		
		HashSet<String> conjFeats = new HashSet<String>();
		// TODO: add more specified conjunction features. i.e. voice(aux+trg)
		HashSet<String> unaryFeats = getUnaryFeatures(
				sentence, propHead, slotId, lattice[slotId][s]);
		conjFeats.addAll(unaryFeats);
		if (slotId > 0) {
			unaryFeats = getUnaryFeatures(
					sentence, propHead, slotId - 1, lattice[slotId-1][sp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		if (slotId > 1) {
			unaryFeats = getUnaryFeatures(
					sentence, propHead, slotId - 2, lattice[slotId-2][spp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		
		if (slotId == QGenSlots.TRGSlotId) {
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
	
	public TIntDoubleHashMap extractTransitionFeatures(
			String[][] lattice, int slotId, int s, int sp, int spp,
			boolean acceptNew) {		
		HashSet<String> conjFeats = new HashSet<String>();
		// TODO: add more specified conjunction features. i.e. voice(aux+trg)
		HashSet<String> unaryFeats = getTransitionFeatures(slotId,
				lattice[slotId][s]);
		conjFeats.addAll(unaryFeats);
		if (slotId > 0) {
			unaryFeats = getTransitionFeatures(slotId - 1,
					lattice[slotId-1][sp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		if (slotId > 1) {
			unaryFeats = getTransitionFeatures(slotId - 2,
					lattice[slotId-2][spp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		
		if (slotId == QGenSlots.TRGSlotId) {
			// TODO...
		}
		conjFeats.add("BIAS");
		
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
	
	public TIntDoubleHashMap extractEmissionFeatures(QGenSequence qGenSequence,
			String[][] lattice, int i, int s, int sp, int spp, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	public void freeze() {
		featureDict = new CountDictionary(featureDict, minFeatureFreq);
	}

}
