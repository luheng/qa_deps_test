package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import util.FeatureUtils;
import data.Corpus;
import data.CountDictionary;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
import experiments.ExperimentUtils;

public class QuestionIdFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	private UniversalPostagMap univDict = null;
	public CountDictionary featureDict = null;
	public final int numBestParses, minFeatureFreq;
	
	public boolean useLexicalFeatures = true;
	public boolean useDependencyFeatures = true;
	
	public QuestionIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq) {
		this.corpus = corpus;
		this.numBestParses = numBestParses;
		this.minFeatureFreq = minFeatureFreq;
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
		univDict = ExperimentUtils.loadPostagMap();
	}
	
	public QuestionIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq,
			boolean useLexicalFeatures, boolean useDependencyFeatures) {
		this(corpus, numBestParses, minFeatureFreq);
		this.useLexicalFeatures = useLexicalFeatures;
		this.useDependencyFeatures = useDependencyFeatures;
	}
	
	private static HashSet<String> getQLabelFeatures(String qlabel) {
		HashSet<String> feats = new HashSet<String>();
		String qkey = qlabel.split("=")[0];    
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
	
	private TIntDoubleHashMap extractFeatures(CountDictionary fdict,
			QASample sample, boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		// *************** Information used to extract features **********
		int length = sample.tokens.length;
		String[] tokens = new String[length];
		for (int i = 0; i < length; i++) {
			tokens[i] = sample.tokens[i].toLowerCase();
		}
		String[] postags = sample.postags;
		
		int propId = sample.propHead;
		String prop = tokens[propId];
		String plemma = inflDict.getBestBaseVerb(prop);
		String pvoice = "Aktv";
		for (TypedDependency dep : FeatureUtils
				.lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		String qlabel = sample.questionLabel;
		String qkey = qlabel.split("=")[0]; 
		String qpp =  qkey.contains("_") ? qkey.split("_")[1] : "";
		HashSet<String> qfeats = getQLabelFeatures(qlabel);
		
		int kBest = useDependencyFeatures ?
				Math.min(numBestParses, sample.kBestParses.size()) : 0;

		// ***************** Proposition features ********************	
		for (String qfeat : qfeats) {
			fv.adjustOrPutValue(fdict.addString("PTOK=" + prop + "#" + qfeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("PLEM=" + plemma + "#" + qfeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
		}
		
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			// Predicate parents
			for (TypedDependency dep : FeatureUtils.lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr + "#" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOVkb=" + govTok + "#" + qfeat, acceptNew), 1, 1);
					if (i == 0) {
						fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PGOV1b=" + govTok + "#" + qfeat, acceptNew), 1, 1);						
					}
				}
			}
			// Predicate children
			for (TypedDependency dep : FeatureUtils.lookupChildrenByParent(deps, propId)) {
				String relStr = dep.reln().toString();
				String modTok = dep.gov().word();
				int modIdx = dep.dep().index() - 1;
				String modPos = sample.postags[modIdx];
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("PCRelkb=" + relStr + "#" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCPoskb=" + modPos + "#" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCTokkb=" + modTok + "#" + qfeat, acceptNew), 1, 1);
					if (modIdx < propId) {
						fv.adjustOrPutValue(fdict.addString("LfCRelkb=" + relStr + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("LfCPoskb=" + modPos + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("LfCTokkb=" + modTok + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
					} else {
						fv.adjustOrPutValue(fdict.addString("RtCRelkb=" + relStr + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("RtCPoskb=" + modPos + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("RtCTokkb=" + modTok + "#PV=" + pvoice + "#" + qfeat, acceptNew), 1, 1);
					}
					if (i == 0) {
						fv.adjustOrPutValue(fdict.addString("PCRel1b=" + relStr + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PCPos1b=" + modPos + "#" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PCTok1b=" + modTok + "#" + qfeat, acceptNew), 1, 1);
					}
				}
			}
		}
		
		// *************** Words in the sentence ****************
		for (int i = 0; i < length; i++) {
			String utag = univDict.getUnivPostag(postags[i]);
			boolean isPP = utag.equals("PRT");
			boolean matchPP = isPP && tokens[i].equals(qpp);
			if (! isPP) {
				continue;
			}
			String rels = "";
			ArrayList<TypedDependency> depPath =
					FeatureUtils.lookupDepPath(sample.kBestParses.get(0), i, propId);
			//if (depPath != null && depPath.size() < 3) {
			rels = FeatureUtils.getRelPathString(depPath, i);
			for (String qfeat : qfeats) {
				fv.adjustOrPutValue(fdict.addString("PP=" + tokens[i] + "#" + qfeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PPRel=" + rels + "#" + qfeat, acceptNew), 1, 1);
				if (matchPP) {
					fv.adjustOrPutValue(fdict.addString("matchPP" + "#" + qfeat, acceptNew), 1, 1);
				}
				if (isPP && i == propId + 1) {
					fv.adjustOrPutValue(fdict.addString("VPP=" + tokens[i] + "#" + qfeat, acceptNew), 1, 1);
					if (matchPP) {
						fv.adjustOrPutValue(fdict.addString("matchVPP" + "#" + qfeat, acceptNew), 1, 1);
					}
					
				}
			}
		}
		
		// *************** QLabel bias *******************
		for (String qfeat : qfeats) {
			fv.adjustOrPutValue(fdict.addString(qfeat, acceptNew), 1, 1);
		}
		fv.remove(-1);
		// Binarize features.
		for (int fid : fv.keys()) {
			fv.put(fid, 1);
		}
		return fv;
	}
	
	public void extractFeatures(ArrayList<QASample> samples) {
		CountDictionary tempFeatureDict = new CountDictionary();
		for (QASample sample : samples) {
			extractFeatures(tempFeatureDict, sample, true /* accept new */);
		}
		
		featureDict = new CountDictionary();
		for (int fid = 0; fid < tempFeatureDict.size(); fid ++) {
			int cnt = tempFeatureDict.getCount(fid);
			if (cnt >= minFeatureFreq) {
				featureDict.addString(tempFeatureDict.getString(fid), cnt);
			}
		}
		System.out.println(String.format(
				"%d features before filtering. %d features after filtering.",
				tempFeatureDict.size(), featureDict.size()));
	}
	
	public TIntDoubleHashMap getFeatures(QASample sample) {
		assert (featureDict != null);
		return extractFeatures(featureDict, sample, false /* accept new */);
	}
	
	public int numFeatures() {
		return featureDict.size();
	}
}
