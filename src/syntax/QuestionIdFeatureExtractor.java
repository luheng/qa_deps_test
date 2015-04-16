package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import annotation.QuestionEncoder;
import data.Corpus;
import data.CountDictionary;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
import experiments.ExperimentUtils;

public class QuestionIdFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	public CountDictionary featureDict = null;
	public final int numBestParses, minFeatureFreq;
	
	public QuestionIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq) {
		this.corpus = corpus;
		this.numBestParses = numBestParses;
		this.minFeatureFreq = minFeatureFreq;
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);
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
		String[] question = sample.question;
		String[] tokens = new String[length];
		for (int i = 0; i < length; i++) {
			tokens[i] = sample.tokens[i].toLowerCase();
		}
		String[] postags = sample.postags;
		
		int propId = sample.propHead;
		String prop = tokens[propId];
		String plemma = inflDict.getBestBaseVerb(prop);
		String wh = question[0].toLowerCase();
		String qlabel = QuestionEncoder.encode(question, tokens).toLowerCase();
		String qvoice = (QuestionEncoder.isPassiveVoice(question) ? "Psv" : "Aktv");
		int kBest = Math.min(numBestParses, sample.kBestParses.size());
		
		// ***************** Proposition features ********************		
		
		// Proposition word and lemma, conditioned question and question label
		fv.adjustOrPutValue(fdict.addString("PTOK_WH=" + prop + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_WH=" + plemma + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_QL=" + prop + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_QL=" + plemma + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_QV=" + prop + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_QV=" + plemma + "_" + qvoice, acceptNew), 1, 1);
		
		// Voice of proposition verb.
		String pvoice = "Aktv";
		for (TypedDependency dep : lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		fv.adjustOrPutValue(fdict.addString("PV_WH=" + pvoice + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_QL=" + pvoice + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_QV=" + pvoice + "_" + qvoice, acceptNew), 1, 1);
		
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			// Parents and parent edge labels of proposition in kbest parses.
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);
				}
			}
			// Children of proposition
			for (TypedDependency dep : lookupChildrenByParent(deps, propId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("PDepFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepFUNkb_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepkb_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepFUNkb_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepkb_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepFUNkb_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PDepkb_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PDepFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDep1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDepFUN1b_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDep1b_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDepFUN1b_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDep1b_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDepFUN1b_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PDep1b_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);
				}
			}
		}
		// TODO: left and right siblings of answer (word and pos)
		// *************** Question *******************
		// Question word and label
		fv.adjustOrPutValue(fdict.addString("WH=" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("QL=" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("QV=" + qvoice, acceptNew), 1, 1);
		
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
