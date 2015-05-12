package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

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
	
	private HashSet<TypedDependency> lookupParentsByChild(
			Collection<TypedDependency> deps, int childId) {
		HashSet<TypedDependency> parents = new HashSet<TypedDependency>();
		for (TypedDependency dep : deps) {
			if (dep.dep().index() == childId + 1) {
				parents.add(dep);
			}
		}
		return parents;
	}
	
	private HashSet<TypedDependency> lookupChildrenByParent(
			Collection<TypedDependency> deps, int parentId) {
		HashSet<TypedDependency> parents = new HashSet<TypedDependency>();
		for (TypedDependency dep : deps) {
			if (dep.gov().index() == parentId + 1) {
				parents.add(dep);
			}
		}
		return parents;
	}
	
	private ArrayList<TypedDependency> lookupDepPath(
			Collection<TypedDependency> deps, int startId, int endId) {
		ArrayList<Integer> queue = new ArrayList<Integer>();
		HashMap<Integer, ArrayList<TypedDependency>> paths  =
				new HashMap<Integer, ArrayList<TypedDependency>>();
		queue.add(startId);
		paths.put(startId, new ArrayList<TypedDependency>());
		while (!queue.isEmpty() && !paths.containsKey(endId)) {
			int currId = queue.get(0), nextId = -1;
			queue.remove(0);
			for (TypedDependency dep : deps) {
				if (dep.gov().index() - 1 == currId) {
					nextId = dep.dep().index() - 1;
				} else if (dep.dep().index() - 1 == currId) {
					nextId = dep.gov().index() - 1;
				}
				if (nextId >= 0 && !paths.containsKey(nextId)) {
					queue.add(nextId);
					ArrayList<TypedDependency> newPath =
							new ArrayList<TypedDependency>();
					newPath.addAll(paths.get(currId));
					newPath.add(dep);
					paths.put(nextId, newPath);
				}
			}
		}
		return paths.get(endId);
	}
	
	private String getRelPathString(ArrayList<TypedDependency> depPath,
			int startId) {
		if (depPath == null) {
			return "-";
		}
		String rels = "";
		int currId = startId;
		for (TypedDependency dep : depPath) {
			if (dep.gov().index() - 1 == currId) {
				currId = dep.dep().index() - 1;
				rels += dep.reln().toString() + "/";
			} else {
				currId = dep.gov().index() - 1;
				rels += dep.reln().toString() + "\\";
			}
		}
		return rels;
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
		
		String qlabel = sample.questionLabel;
		String qkey = qlabel.split("=")[0];    
		String qval = qlabel.split("=")[1];    // i.e. someone, something
		String qcat = qkey.contains("_") ? qkey.split("_")[0] : qkey; // i.e. ARG0, 
		String qpp =  qkey.contains("_") ? qkey.split("_")[1] : "";
		String[] qlabelInfo = qlabel.split("_");
		
		int kBest = useDependencyFeatures ?
				Math.min(numBestParses, sample.kBestParses.size()) : 0;
		
		// ***************** Proposition features ********************		
		String pvoice = "Aktv";
		for (TypedDependency dep : lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		
		// TODO: possible features
		// 1. predicate, lemma, voice
		// 2. predicate children
		// 3. all nouns in the sentence
		// 4. predicate voice
		// 5. pp in qlabel
		// 6. negation in sentence
		
		fv.adjustOrPutValue(fdict.addString("PTOK_QL=" + prop + "_" + qkey, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_iF=" + prop + "_" + qval, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_QL=" + prop + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_WH=" + plemma + "_" + qkey, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_IF=" + plemma + "_" + qval, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_QL=" + plemma + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_WH=" + pvoice + "_" + qkey, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_WH=" + pvoice + "_" + qval, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_QL=" + pvoice + "_" + qlabel, acceptNew), 1, 1);
		
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			// Predicate parents
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_WH=" + relStr + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_WH=" + govTok + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_IF=" + relStr + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_IF=" + govTok + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_WH=" + relStr + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_WH=" + govTok + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_IF=" + relStr + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_IF=" + govTok + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
					
				}
			}
			// Predicate children
			for (TypedDependency dep : lookupChildrenByParent(deps, propId)) {
				String relStr = dep.reln().toString();
				String modTok = dep.gov().word();
				String modPos = sample.postags[dep.dep().index() - 1];
				fv.adjustOrPutValue(fdict.addString("PCRelkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCPoskb=" + modPos, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCTokkb=" + modTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCRelkb_WH=" + relStr + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCPoskb_WH=" + modPos + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCTokkb_WH=" + modTok + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCRelkb_IF=" + relStr + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCPoskb_IF=" + modPos + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCTokkb_IF=" + modTok + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCRelkb_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCPoskb_QL=" + modPos + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PCTokkb_QL=" + modTok + "_" + qlabel, acceptNew), 1, 1);
			
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PCRel1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCPos1b=" + modPos, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCTok1b=" + modTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCRel1b_WH=" + relStr + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCPos1b_WH=" + modPos + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCTok1b_WH=" + modTok + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCRel1b_IF=" + relStr + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCPos1b_IF=" + modPos + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCTok1b_IF=" + modTok + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCRel1b_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCPos1b_QL=" + modPos + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PCTok1b_QL=" + modTok + "_" + qlabel, acceptNew), 1, 1);
				
				}
			}
		}
		
		// *************** Words in the sentence ****************
		for (int i = 0; i < length; i++) {
			String utag = univDict.getUnivPostag(postags[i]);
			if (utag.equals("PRT")) {
				boolean matchPP = qlabel.contains(tokens[i]);
				fv.adjustOrPutValue(fdict.addString("hasPP_WH=" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("hasPP_IF=" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("hasPP_QL=" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PP_WH=" + tokens[i] + "_" + qkey, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PP_IF=" + tokens[i] + "_" + qval, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PP_QL=" + tokens[i] + "_" + qlabel, acceptNew), 1, 1);
				if (matchPP) {
					fv.adjustOrPutValue(fdict.addString("matchPP_WH=" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("matchPP_IF=" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("matchPP_QL=" + qlabel, acceptNew), 1, 1);
				}
				if (i < propId) {
					fv.adjustOrPutValue(fdict.addString("hasPPleft_WH=" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("hasPPleft_IF=" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("hasPPleft_QL=" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPleft_WH=" + tokens[i] + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPleft_IF=" + tokens[i] + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPleft_QL=" + tokens[i] + "_" + qlabel, acceptNew), 1, 1);
					if (matchPP) {
						fv.adjustOrPutValue(fdict.addString("matchPleft_WH=" + qkey, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("matchPleft_IF=" + qval, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("matchPPleft_QL=" + qlabel, acceptNew), 1, 1);
					}
				} else {
					fv.adjustOrPutValue(fdict.addString("hasPPright_WH=" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("hasPPright_IF=" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("hasPPright_QL=" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPright_WH=" + tokens[i] + "_" + qkey, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPright_IF=" + tokens[i] + "_" + qval, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PPright_QL=" + tokens[i] + "_" + qlabel, acceptNew), 1, 1);
					if (matchPP) {
						fv.adjustOrPutValue(fdict.addString("matchPPright_WH=" + qkey, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("matchPPright_IF=" + qval, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("matchPPright_QL=" + qlabel, acceptNew), 1, 1);
					}
				}
			}
		}
		
		// *************** Question *******************
		// Question word and label
		fv.adjustOrPutValue(fdict.addString("WH=" + qkey, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("IF=" + qval, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("QL=" + qlabel, acceptNew), 1, 1);
		
		// *************** Bias feature ... ***************
		fv.adjustOrPutValue(fdict.addString("BIAS=.", acceptNew), 1, 1);
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
		System.out.println(String.format("%d features before filtering. %d features after filtering.",
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
