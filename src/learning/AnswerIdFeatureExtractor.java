package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import data.Corpus;
import data.CountDictionary;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
import experiments.ExperimentUtils;

public class AnswerIdFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	public CountDictionary featureDict = null;
	public final int numBestParses, minFeatureFreq;
	
	public boolean useLexicalFeatures = true;
	public boolean useDependencyFeatures = true;
	public boolean use1BestFeatures = false;
	
	public AnswerIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq) {
		this.corpus = corpus;
		this.numBestParses = numBestParses;
		this.minFeatureFreq = minFeatureFreq;
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
	}
	
	public AnswerIdFeatureExtractor(Corpus corpus, int numBestParses,
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
	
	private static HashSet<String> getQLabelFeatures(String qlabel) {
		HashSet<String> feats = new HashSet<String>();
		String qkey = qlabel.split("=")[0];    
		String qval = qlabel.split("=")[1];    // i.e. someone, something
		String wh = qkey.split("_")[0];
		if (wh.startsWith("A")) {
			wh = qval.equals("someone") ? "WHO" : "WHAT";
		}
		feats.add("WH=" + wh);
		feats.add("QLab=" + qkey);
		feats.add("QLab*=" + qlabel);
		return feats;
	}
	
	private TIntDoubleHashMap extractFeatures(CountDictionary fdict,
			QASample sample, boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		
		int length = sample.tokens.length;
		String[] tokens = new String[length];
		for (int i = 0; i < length; i++) {
			tokens[i] = sample.tokens[i].toLowerCase();
		}
		String[] postags = sample.postags;
		
		int propId = sample.propHead;
		int answerId = sample.answerWordPosition;
		String aposFeat = "APOS=" + postags[answerId];
		String atokFeat = "ATOK=" + tokens[answerId];
		String plemFeat = "PLEM=" + inflDict.getBestBaseVerb(tokens[propId]);
		String ptokFeat = "PTOK=" + tokens[propId];
		int kBest = useDependencyFeatures ?
				Math.min(numBestParses, sample.kBestParses.size()) : 0;
		
		String pvoice = "Aktv";
		for (TypedDependency dep : lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		String pvFeat = "PV=" + pvoice;
		String relPos = (answerId < propId ? "left" : "right");
		
		HashSet<String> qfeats = getQLabelFeatures(sample.questionLabel);
		qfeats.add("BIAS=.");
		
		// Proposition word and lemma, conditioned question and question label
		// Argument word and pos, conditioned on question word and label
		for (String qfeat : qfeats) {
			fv.adjustOrPutValue(fdict.addString(qfeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString(qfeat + "_" + aposFeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString(plemFeat + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString(pvFeat + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString(relPos + "_" + qfeat, acceptNew), 1, 1);
			if (useLexicalFeatures) {
				fv.adjustOrPutValue(fdict.addString(qfeat + "_" + atokFeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString(ptokFeat + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString(ptokFeat + "_" + qfeat + "_" + atokFeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString(plemFeat + "_" + qfeat + "_" + atokFeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString(pvFeat + "_" + qfeat + "_" + atokFeat, acceptNew), 1, 1);
			}
		}
		
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);			
			// Parents and parent edge labels of proposition in kbest parses.
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govPos = dep.gov().index() <= 0 ? "ROOT" : postags[dep.gov().index() - 1];
				String govTok = dep.gov().word();
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGPoskb=" + govPos + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGPoskb=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					if (i == 0 && use1BestFeatures) {
						fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PGPos1b=" + govPos + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PGPos1b=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					}
					if (useLexicalFeatures) {
						fv.adjustOrPutValue(fdict.addString("PGTokkb=" + govTok + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PGTokkb=" + govTok + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						if (i == 0 && use1BestFeatures) {
							fv.adjustOrPutValue(fdict.addString("PGTok1b=" + govTok + "_" + qfeat, acceptNew), 1, 1);
							fv.adjustOrPutValue(fdict.addString("PGTok1b=" + govTok + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						}
					}
				}
			}

			// ****************** Argument syntactic context ************************
			for (TypedDependency dep : lookupParentsByChild(deps, answerId)) {
				String relStr = dep.reln().toString();
				String govPos = dep.gov().index() <= 0 ? "ROOT" : postags[dep.gov().index() - 1];
				String govTok = dep.gov().word();
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGPoskb=" + govPos + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("aGPoskb=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					if (i == 0 && use1BestFeatures) {
						fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("AGPos1b=" + govPos + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("AGPos1b=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					}
					if (dep.gov().index() == propId + 1) {
						fv.adjustOrPutValue(fdict.addString("PisPr(A)=True=" + "_" + qfeat, acceptNew), 1, 1);						
						fv.adjustOrPutValue(fdict.addString("PisPr(A)=" + relStr + "_" + qfeat, acceptNew), 1, 1);
					}
					if (useLexicalFeatures) {
						fv.adjustOrPutValue(fdict.addString("AGTokkb=" + govTok + "_" + qfeat, acceptNew), 1, 1);
						if (i == 0 && use1BestFeatures) {
							fv.adjustOrPutValue(fdict.addString("AGTok1b=" + govTok + "_" + qfeat, acceptNew), 1, 1);
						}
					}
				}
			}

			// Leftmost and rightmost children of answer head.
			int leftMostChild = length, rightMostChild = -1;
			for (TypedDependency dep : lookupChildrenByParent(deps, answerId)) {
				int depId = dep.dep().index() - 1;
				leftMostChild = Math.min(leftMostChild, depId);
				rightMostChild = Math.max(rightMostChild, depId);
			}
			String lcTok = leftMostChild < length ? tokens[leftMostChild] : "LEAF";
			String lcPos = leftMostChild < length ? postags[leftMostChild] : "LEAF";
			String rcTok = rightMostChild > -1 ? tokens[rightMostChild] : "LEAF";
			String rcPos = rightMostChild > - 1 ? postags[rightMostChild] : "LEAF";
			for (String qfeat : qfeats) {
				fv.adjustOrPutValue(fdict.addString("ALChPOSkb=" + lcPos + "_" + qfeat, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOSkb=" + rcPos + "_" + qfeat, acceptNew), 1, 1);
				if (i == 0 && use1BestFeatures) {
					fv.adjustOrPutValue(fdict.addString("ALChPOS1b=" + lcPos + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("ARChPOS1b=" + rcPos + "_" + qfeat, acceptNew), 1, 1);
				}
				if (useLexicalFeatures) {
					fv.adjustOrPutValue(fdict.addString("ALChTOKkb=" + lcTok + "_" + qfeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("ARChTOKkb=" + rcTok + "_" + qfeat, acceptNew), 1, 1);
					if (i == 0 && use1BestFeatures) {
						fv.adjustOrPutValue(fdict.addString("ALChTOK1b=" + lcTok + "_" + qfeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("ARChTOK1b=" + rcTok + "_" + qfeat, acceptNew), 1, 1);
					}
				}
			}

			// Syntactic path between proposition and argument
			ArrayList<TypedDependency> depPath = lookupDepPath(deps, answerId, propId);
			String rels = getRelPathString(depPath, answerId);
			for (String qfeat : qfeats) {
				fv.adjustOrPutValue(fdict.addString("RelPathkb=" + rels + "_" + qfeat, acceptNew), 1, 1);
				if (i == 0 && use1BestFeatures) {
					fv.adjustOrPutValue(fdict.addString("RelPath1b=" + rels + "_" + qfeat, acceptNew), 1, 1);
				}
			}
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
