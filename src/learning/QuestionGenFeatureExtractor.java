package learning;

import java.util.ArrayList;
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
	
	private static ArrayList<String> getWhFeatures(String wh) {
		ArrayList<String> feats = new ArrayList<String>();
		feats.add("WH=" + wh);
		return feats;
	}
	
	private static ArrayList<String> getAuxFeatures(String aux) {
		ArrayList<String> feats = new ArrayList<String>();
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
	
	private static ArrayList<String> getPhFeatures(String ph, String prefix) {
		ArrayList<String> feats = new ArrayList<String>();
		if (ph.isEmpty()) {
			feats.add(prefix + "=null");
		} else {
			feats.add(prefix + "=" + ph);
		}
		// TODO: add ph3 options
		return feats;
	}
	
	public TIntDoubleHashMap extractFeatures(Sentence sentence, int propHead,
			String[][] lattice, boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		// WH
		for (int i = 0; i < lattice[0].length; i++) {
			for (String feat : getWhFeatures(lattice[0][i])) {
				fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
			}
		}
		// AUX
		for (int i = 0; i < lattice[1].length; i++) {
			for (String feat : getAuxFeatures(lattice[1][i])) {
				fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
			}
		}
		// PH1
		for (int i = 0; i < lattice[2].length; i++) {
			for (String feat : getPhFeatures(lattice[2][i], "PH1")) {
				fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
			}
		}
		// TRG
		// PH2
		for (int i = 0; i < lattice[4].length; i++) {
			for (String feat : getPhFeatures(lattice[4][i], "PH2")) {
				fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
			}
		}
		// PP
		// PH3
		for (int i = 0; i < lattice[6].length; i++) {
			for (String feat : getPhFeatures(lattice[6][i], "PH3")) {
				fv.adjustOrPutValue(featureDict.addString(feat, acceptNew), 1, 1);
			}
		}
		// TODO: add bias term.
		fv.remove(-1);
		// Binarize features.
		for (int fid : fv.keys()) {
			fv.put(fid, 1);
		}
		return fv;
	}
}
