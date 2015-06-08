package qg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import util.FeatureUtils;
import learning.QASample;
import annotation.QASlotPrepositions;
import annotation.QASlots;
import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
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
		this.inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
		this.univDict = ExperimentUtils.loadPostagMap();
		this.featureDict = new CountDictionary();
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
		return feats;
	}
	
	private static HashSet<String> getPPFeatures(String pp) {
		HashSet<String> feats = new HashSet<String>();
		if (pp.isEmpty()) {
			feats.add("PP=null");
		} else {
			feats.add("PP=" + pp);
		}
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
	
	public TIntDoubleHashMap extractTransitionFeatures(
			String[][] lattice, int slotId, int s, int sp, int spp,
			boolean acceptNew) {		
		HashSet<String> conjFeats = new HashSet<String>();
		// TODO: add more specified conjunction features. i.e. voice(aux+trg)
		HashSet<String> unaryFeats = getUnaryFeatures(slotId,
				lattice[slotId][s]);
		conjFeats.addAll(unaryFeats);
		if (slotId > 0) {
			unaryFeats = getUnaryFeatures(slotId - 1, lattice[slotId-1][sp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		if (slotId > 1) {
			unaryFeats = getUnaryFeatures(slotId - 2, lattice[slotId-2][spp]);
			makeConjunctionFeatures(unaryFeats, conjFeats);
		}
		if (slotId == QASlots.TRGSlotId) {
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
	
	public TIntDoubleHashMap extractEmissionFeatures(
			QGenSequence sequence,  String[][] lattice, int slotId, int s,
			boolean acceptNew) {
		QASample sample = sequence.sample;
		ArrayList<Collection<TypedDependency>> parses = sample.kBestParses;
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		
		Sentence sent = sequence.sentence;
		int propHead = sequence.propHead;
		String prop = sent.getTokenString(propHead).toLowerCase();
		String[] infl = inflDict.getBestInflections(prop);
		String plemma = infl[0];
		String pvoice = "Aktv";
		for (TypedDependency dep : FeatureUtils
				.lookupChildrenByParent(sample.kBestParses.get(0), propHead)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		
		HashSet<String> unaryFeats = getUnaryFeatures(sent, propHead,slotId,
				lattice[slotId][s]);
				
		// ***************** Proposition features ********************	
		for (String feat : unaryFeats) {
			fv.adjustOrPutValue(featureDict.addString("PTOK=" + prop + "#" + feat, acceptNew), 1, 1);
			fv.adjustOrPutValue(featureDict.addString("PLEM=" + plemma + "#" + feat, acceptNew), 1, 1);
			fv.adjustOrPutValue(featureDict.addString("PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
		}
		
		for (int i = 0; i < parses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			// Predicate parents
			for (TypedDependency dep : FeatureUtils.lookupParentsByChild(deps, propHead)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				for (String feat : unaryFeats) {
					fv.adjustOrPutValue(featureDict.addString("PFUNkb=" + relStr + "#" + feat, acceptNew), 1, 1);
					fv.adjustOrPutValue(featureDict.addString("PGOVkb=" + govTok + "#" + feat, acceptNew), 1, 1);
				}
			}
			// Predicate children
			for (TypedDependency dep : FeatureUtils.lookupChildrenByParent(deps, propHead)) {
				String relStr = dep.reln().toString();
				String modTok = dep.gov().word();
				int modIdx = dep.dep().index() - 1;
				String modPos = sample.postags[modIdx];
				for (String feat : unaryFeats) {
					fv.adjustOrPutValue(featureDict.addString("PCRelkb=" + relStr + "#" + feat, acceptNew), 1, 1);
					fv.adjustOrPutValue(featureDict.addString("PCPoskb=" + modPos + "#" + feat, acceptNew), 1, 1);
					fv.adjustOrPutValue(featureDict.addString("PCTokkb=" + modTok + "#" + feat, acceptNew), 1, 1);
					if (modIdx < propHead) {
						fv.adjustOrPutValue(featureDict.addString("LfCRelkb=" + relStr + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
						fv.adjustOrPutValue(featureDict.addString("LfCPoskb=" + modPos + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
						fv.adjustOrPutValue(featureDict.addString("LfCTokkb=" + modTok + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
					} else {
						fv.adjustOrPutValue(featureDict.addString("RtCRelkb=" + relStr + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
						fv.adjustOrPutValue(featureDict.addString("RtCPoskb=" + modPos + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
						fv.adjustOrPutValue(featureDict.addString("RtCTokkb=" + modTok + "#PV=" + pvoice + "#" + feat, acceptNew), 1, 1);
					}
				}
			}
		}
		
		// *************** Words in the sentence ****************
		if (slotId == QASlots.PPSlotId) {
			String pp = lattice[slotId][s];
			for (int i = 0; i < sent.length; i++) {
				String utag = univDict.getUnivPostag(sequence.sample.postags[i]);
				boolean isPP = utag.equals("PRT");
				if (!isPP) {
					continue;
				}
				String ppTok = sent.getTokenString(i);
				boolean matchPP = isPP && ppTok.equals(pp);
				String rels = "";
				ArrayList<TypedDependency> depPath = FeatureUtils.lookupDepPath(
						sample.kBestParses.get(0), i, propHead);
				rels = FeatureUtils.getRelPathString(depPath, i);
				for (String feat : unaryFeats) {
					fv.adjustOrPutValue(featureDict.addString("PP=" + ppTok + "#" + feat, acceptNew), 1, 1);
					fv.adjustOrPutValue(featureDict.addString("PPRel=" + rels + "#" + feat, acceptNew), 1, 1);
					if (matchPP) {
						fv.adjustOrPutValue(featureDict.addString("matchPP" + "#" + feat, acceptNew), 1, 1);
					}
					if (isPP && i == propHead + 1) {
						fv.adjustOrPutValue(featureDict.addString("VPP=" + ppTok + "#" + feat, acceptNew), 1, 1);
						if (matchPP) {
							fv.adjustOrPutValue(featureDict.addString("matchVPP" + "#" + feat, acceptNew), 1, 1);
						}
						
					}
				}
			}
		}
		
		fv.remove(-1);
		for (int fid : fv.keys()) {
			fv.put(fid, 1);
		}
		return fv;
	}

	public int numFeatures() {
		return featureDict.size();
	}

	public void pruneFeatures() {
		featureDict = new CountDictionary(featureDict, minFeatureFreq);
	}

}
