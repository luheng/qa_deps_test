package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import annotation.QuestionEncoder;
import data.Corpus;
import data.CountDictionary;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
import experiments.ExperimentUtils;

public class KBestFeatureExtractor {
	private Corpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	public CountDictionary featureDict = null;
	public final int minFeatureFreq;
	
	public KBestFeatureExtractor(Corpus corpus, int minFeatureFreq) {
		this.corpus = corpus;
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
		
		// ***************** Proposition features ********************		
		
		// Proposition word and lemma, conditioned question and question label
		fv.adjustOrPutValue(fdict.addString("PTOK=" + prop, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM=" + plemma, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_WH=" + prop + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_WH=" + plemma + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_QLab=" + prop + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_QLab=" + plemma + "_" + qlabel, acceptNew), 1, 1);
		
		// Parents and parent edge labels of proposition in kbest parses.
		for (int i = 0; i < sample.kBestParses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);			
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PFUNkb_QLab=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb_QLab=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PFUN1b_QLab=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b_QLab=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				}
			}
		}
		// TODO: Voice of proposition (active or passive)
		
		//*******************  Argument features **********************
		
		// Argument word and pos, conditioned on question word and label
		fv.adjustOrPutValue(fdict.addString("ATOK=" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS=" + apos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_WH=" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_WH=" + apos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_QLab=" + apos + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_QLab=" + answer + "_" + qlabel, acceptNew), 1, 1);
		
		// ****************** Argument syntactic context ************************
		
		// Parents and parent edge labels of answer head in kbest parses.
		// Leftmost and rightmost children of answer head.
		for (int i = 0; i < sample.kBestParses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			for (TypedDependency dep : lookupParentsByChild(deps, answerId)) {
				String relStr = dep.reln().toString();
				String govTok = dep.gov().word();
				fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb=" + govTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AFUNkb_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AFUNkb_QLab=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb_QLab=" + govTok + "_" + qlabel, acceptNew), 1, 1);

				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b=" + govTok, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUN1b_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b_WH=" + govTok + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AFUN1b_QLab=" + relStr + "_" + qlabel, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b_QLab=" + govTok + "_" + qlabel, acceptNew), 1, 1);
				}
				// If proposition is syntactic parent of answer in any of the k-best parses
				if (dep.gov().index() == propId + 1) {
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True", acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True_WH=" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True_QLab=" + qlabel, acceptNew), 1, 1);
					
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=" + relStr, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)_WH=" + relStr + "_" + wh, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)_QLab=" + relStr + "_" + qlabel, acceptNew), 1, 1);
				}
			}
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
			fv.adjustOrPutValue(fdict.addString("ALChTOKkb_QLab=" + lcTok + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ALChPOSkb_QLab=" + lcPos + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChTOKkb_QLab=" + rcTok + "_" + qlabel, acceptNew), 1, 1);
			fv.adjustOrPutValue(fdict.addString("ARChPOSkb_QLab=" + rcPos + "_" + qlabel, acceptNew), 1, 1);
			
			if (i == 0) {
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b=" + lcTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b=" + lcPos, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b=" + rcTok, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b=" + rcPos, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b_WH=" + lcTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b_WH=" + lcPos + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b_WH=" + rcTok + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b_WH=" + rcPos + "_" + wh, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChTOK1b_QLab=" + lcTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ALChPOS1b_QLab=" + lcPos + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChTOK1b_QLab=" + rcTok + "_" + qlabel, acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ARChPOS1b_QLab=" + rcPos + "_" + qlabel, acceptNew), 1, 1);
			}
		}
		
		// TODO: left and right siblings of answer (word and pos)
		
		// **************** Proposition-Argument relation *******************
		// Relative position
		String relPos = (answerId < propId ? "left" : "right");
		fv.adjustOrPutValue(fdict.addString("RelPos=" + relPos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("RelPos_WH=" + relPos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("RelPos_QLab=" + relPos + "_" + qlabel, acceptNew), 1, 1);
		
		// TODO: syntactic path between proposition and argument
		
		// *************** Question *******************
		// Question word and label
		fv.adjustOrPutValue(fdict.addString("WH=" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("QLabel=" + qlabel, acceptNew), 1, 1);
		
		// *************** Bias feature ... ***************
		fv.adjustOrPutValue(fdict.addString("BIAS=.", acceptNew), 1, 1);
		fv.remove(-1);
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
