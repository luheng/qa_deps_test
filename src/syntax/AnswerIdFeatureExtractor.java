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

public class AnswerIdFeatureExtractor {
	@SuppressWarnings("unused")
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	public CountDictionary featureDict = null;
	public final int numBestParses, minFeatureFreq;
	
	public AnswerIdFeatureExtractor(Corpus corpus, int numBestParses,
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
		int answerId = sample.answerHead;
		String prop = tokens[propId];
		String plemma = inflDict.getBestBaseVerb(prop);
		String answer = tokens[answerId];
		String apos = postags[answerId];
		String wh = question[0].toLowerCase();
		String qlabel = QuestionEncoder.encode(question, tokens).toLowerCase();
		String qvoice = (QuestionEncoder.isPassiveVoice(question) ? "Psv" : "Aktv");
		int kBest = Math.min(numBestParses, sample.kBestParses.size());
		
		// ***************** Proposition features ********************		
		
		// Proposition word and lemma, conditioned question and question label
		fv.adjustOrPutValue(fdict.addString("PTOK_ATOK=" + prop + "_" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_APOS=" + prop + "_" + apos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_ATOK=" + plemma + "_" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_APOS=" + plemma + "_" + apos, acceptNew), 1, 1);

		fv.adjustOrPutValue(fdict.addString("PTOK_ATOK_WH=" + prop + "_" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_APOS_WH=" + prop + "_" + apos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_ATOK_WH=" + plemma + "_" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_APOS_WH=" + plemma + "_" + apos + "_" + wh, acceptNew), 1, 1);

		fv.adjustOrPutValue(fdict.addString("PTOK_ATOK_QL=" + prop + "_" + answer + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_APOS_QL=" + prop + "_" + apos + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_ATOK_QL=" + plemma + "_" + answer + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_APOS_QL=" + plemma + "_" + apos + "_" + qlabel, acceptNew), 1, 1);

		fv.adjustOrPutValue(fdict.addString("PTOK_ATOK_QV=" + prop + "_" + answer + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_APOS_QV=" + prop + "_" + apos + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_ATOK_QV=" + plemma + "_" + answer + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_APOS_QV=" + plemma + "_" + apos + "_" + qvoice, acceptNew), 1, 1);
		
		// Voice of proposition verb.
		String pvoice = "Aktv";
		for (TypedDependency dep : lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		fv.adjustOrPutValue(fdict.addString("PV_ATOK=" + pvoice + "_" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_APOS=" + pvoice + "_" + apos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_ATOK_WH=" + pvoice + "_" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_APOS_WH=" + pvoice + "_" + apos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_ATOK_QL=" + pvoice + "_" + answer + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_APOS_QL=" + pvoice + "_" + apos + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_ATOK_QV=" + pvoice + "_" + answer + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PV_APOS_QV=" + pvoice + "_" + apos + "_" + qvoice, acceptNew), 1, 1);
		
		// Parents and parent edge labels of proposition in kbest parses.
		/*
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);			
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
		}
		*/
		
		//*******************  Argument features **********************
		
		// Argument word and pos, conditioned on question word and label
		fv.adjustOrPutValue(fdict.addString("ATOK=" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS=" + apos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_WH=" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_WH=" + apos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_QL=" + apos + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_QL=" + answer + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_QV=" + apos + "_" + qvoice, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_QV=" + answer + "_" + qvoice, acceptNew), 1, 1);
		
		// ****************** Argument syntactic context ************************
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			// Parents and parent edge labels of answer head in kbest parses.
			for (TypedDependency dep : lookupParentsByChild(deps, answerId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AFUNkb_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AFUNkb_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AFUNkb_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);

				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUN1b_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUN1b_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b_QL=" + govTok + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUN1b_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b_QV=" + govTok + "_" + qvoice, acceptNew), 1, 1);
				}
				// If proposition is syntactic parent of answer in any of the k-best parses
				if (dep.gov().index() == propId + 1) {
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True", acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True_WH=" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True_QL=" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True_QV=" + qvoice, acceptNew), 1, 1);
					
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)_QL=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)_QV=" + relStr + "_" + qvoice, acceptNew), 1, 1);
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
			
			fv.adjustOrPutValue(fdict.addString("ALChTOKkb=" + lcTok, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChPOSkb=" + lcPos, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChTOKkb=" + rcTok, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChPOSkb=" + rcPos, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChTOKkb_WH=" + lcTok + "_" + wh, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChPOSkb_WH=" + lcPos + "_" + wh, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChTOKkb_WH=" + rcTok + "_" + wh, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChPOSkb_WH=" + rcPos + "_" + wh, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChTOKkb_QL=" + lcTok + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChPOSkb_QL=" + lcPos + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChTOKkb_QL=" + rcTok + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChPOSkb_QL=" + rcPos + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChTOKkb_QV=" + lcTok + "_" + qvoice, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChPOSkb_QV=" + lcPos + "_" + qvoice, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChTOKkb_QV=" + rcTok + "_" + qvoice, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChPOSkb_QV=" + rcPos + "_" + qvoice, acceptNew), 1, 1);

			if (i == 0) {
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b=" + lcTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b=" + lcPos, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b=" + rcTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b=" + rcPos, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b_WH=" + lcTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b_WH=" + lcPos + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b_WH=" + rcTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b_WH=" + rcPos + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b_QL=" + lcTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b_QL=" + lcPos + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b_QL=" + rcTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b_QL=" + rcPos + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b_QV=" + lcTok + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b_QV=" + lcPos + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b_QV=" + rcTok + "_" + qvoice, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b_QV=" + rcPos + "_" + qvoice, acceptNew), 1, 1);

			}
		}
		
		// TODO: left and right siblings of answer (word and pos)
		
		// **************** Proposition-Argument relation *******************
		// Relative position
		String relPos = (answerId < propId ? "left" : "right");
		fv.adjustOrPutValue(fdict.addString("RelPos=" + relPos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("RelPos_WH=" + relPos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("RelPos_QL=" + relPos + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("RelPos_QV=" + relPos + "_" + qvoice, acceptNew), 1, 1);
		
		// Syntactic path between proposition and argument
		for (int i = 0; i < kBest; i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			ArrayList<TypedDependency> depPath = lookupDepPath(deps, answerId, propId);
			String rels = getRelPathString(depPath, answerId);
			//System.out.println(i + "\t" + rels);
			fv.adjustOrPutValue(fdict.addString("RelPathkb=" + rels, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("RelPathkb_WH=" + rels + "_" + wh, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("RelPathkb_QL=" + rels + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("RelPathkb_QV=" + rels + "_" + qvoice, acceptNew), 1, 1);
			
			if (i == 0) {
				fv.adjustOrPutValue(fdict.addString("RelPath1b=" + rels, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("RelPath1b_WH=" + rels + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("RelPath1b_QL=" + rels + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("RelPath1b_QV=" + rels + "_" + qvoice, acceptNew), 1, 1);
			}
		}
		
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
