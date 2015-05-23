package learning;

import java.util.ArrayList;
import java.util.HashSet;

import data.Corpus;
import data.CountDictionary;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class QAFeatureExtractor {
	protected Corpus corpus = null;
	protected VerbInflectionDictionary inflDict = null;
	protected UniversalPostagMap univDict = null;
	protected TIntIntHashMap featureFreq = null;
	
	public CountDictionary featureDict = null;
	public final int numBestParses, minFeatureFreq;
	public boolean useLexicalFeatures = true;
	public boolean useDependencyFeatures = true;
	public boolean use1BestFeatures = true;
	
	public QAFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq) {
		this.corpus = corpus;
		this.numBestParses = numBestParses;
		this.minFeatureFreq = minFeatureFreq;
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
		univDict = ExperimentUtils.loadPostagMap();
	}
	
	public QAFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq, boolean useLexicalFeatures,
			boolean useDependencyFeatures, boolean use1BestFeatures) {
		this(corpus, numBestParses, minFeatureFreq);
		this.useLexicalFeatures = useLexicalFeatures;
		this.useDependencyFeatures = useDependencyFeatures;
		this.use1BestFeatures = use1BestFeatures;
	}
	
	protected TIntDoubleHashMap extractFeatures(CountDictionary fdict,
			QASample sample, boolean acceptNew) {
		return null;
	}

	protected static HashSet<String> getQLabelFeatures(String qlabel) {
		HashSet<String> feats = new HashSet<String>();
		String qkey = qlabel.split("=")[0];    // i.e. W2_to
		String qval = qlabel.split("=")[1];    // i.e. someone, something
		String qtype = qkey.contains("_") ? qkey.split("_")[0] : qkey; // i.e. W0, 
		String qpp =  qkey.contains("_") ? qkey.split("_")[1] : "";
		feats.add("QKey=" + qkey);
		feats.add("QType=" + qtype);
		if (!qpp.isEmpty()) {
			feats.add("QPP=" + qpp);
		}
		feats.add("QVal=" + qval);
		feats.add("QLab=" + qlabel);
		return feats;
	}
	
	public void extractFeatures(ArrayList<QASample> samples) {
		CountDictionary tempFeatureDict = new CountDictionary();
		featureFreq = new TIntIntHashMap();
		for (QASample sample : samples) {
			extractFeatures(tempFeatureDict, sample, true /* accept new */);
		}
		featureDict = new CountDictionary();
		for (int fid = 0; fid < tempFeatureDict.size(); fid ++) {
			int cnt = featureFreq.get(fid);
			if (cnt >= minFeatureFreq) {
				featureDict.addString(tempFeatureDict.getString(fid), cnt);
			}
		}
		System.out.println(String.format(
				"%d features before filtering. %d features after filtering.",
				tempFeatureDict.size(), featureDict.size()));
		featureFreq = null;
	}
	
	public TIntDoubleHashMap getFeatures(QASample sample) {
		assert (featureDict != null);
		return extractFeatures(featureDict, sample, false /* accept new */);
	}
	
	public int numFeatures() {
		return featureDict.size();
	}
}
