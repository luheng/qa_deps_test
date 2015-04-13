package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import data.Corpus;
import data.CountDictionary;
import data.Sentence;
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
	
	private HashSet<TypedDependency> lookupChildrenByparent(
			Collection<TypedDependency> deps, int parentId) {
		HashSet<TypedDependency> parents = new HashSet<TypedDependency>();
		for (TypedDependency dep : deps) {
			if (dep.gov().index() == parentId + 1) {
				parents.add(dep);
			}
		}
		return parents;
	}
	
	private String getToken(Sentence sent, int id) {
		return id < 0 ? "ROOT" : sent.getTokenString(id);
	}
	
	private TIntDoubleHashMap extractFeatures(CountDictionary fdict,
			QASample sample, boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		
		// *************** Information used to extract features **********
		
		Sentence sent = sample.sentence;
		String[] tokens = new String[sent.length];
		for (int i = 0; i < sent.length; i++) {
			tokens[i] = sent.getTokenString(i).toLowerCase();
		}
		
		int propId = sample.propHead;
		int answerId = sample.answerHead;
		String prop = tokens[propId];
		String plemma = inflDict.getBestBaseVerb(prop);
		String answer = tokens[answerId];
		String apos = sample.postags[answerId];
		String wh = sample.qa.questionWords[0].toLowerCase();
		String qlabel = sample.qa.getQuestionLabel().toLowerCase();
		
		// ***************** Proposition features ********************		
		
		// Proposition word and lemma, conditioned question and question label
		fv.adjustOrPutValue(fdict.addString("PTOK=" + prop, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM=" + plemma, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_WH=" + prop + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PTOK_QLab=" + prop + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_WH=" + plemma + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("PLEM_QLab=" + plemma + "_" + qlabel, acceptNew), 1, 1);
		
		// Parents and parent edge labels of proposition in kbest parses.
		for (int i = 0; i < sample.kBestParses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				fv.adjustOrPutValue(fdict.addString("PFUNkb=" + dep.reln(), acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("PGOVkb=" + dep.gov().word(), acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("PFUN1b=" + dep.reln(), acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGOV1b=" + dep.gov().word(), acceptNew), 1, 1);
				}
			}
		}
		// TODO: Voice of proposition (active or passive)
		
		//*******************  Argument features **********************
		
		// Argument word and pos, conditioned on question word and label
		fv.adjustOrPutValue(fdict.addString("ATOK=" + answer, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS=" + apos, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_WH=" + answer + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("ATOK_QLab=" + answer + "_" + qlabel, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_WH=" + apos + "_" + wh, acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("APOS_QLab=" + apos + "_" + qlabel, acceptNew), 1, 1);
		// TODO: Argument lemma
		
		
		// Parents and parent edge labels of answer head in kbest parses.
		for (int i = 0; i < sample.kBestParses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			for (TypedDependency dep : lookupParentsByChild(deps, answerId)) {
				fv.adjustOrPutValue(fdict.addString("AFUNkb=" + dep.reln(), acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("AGOVkb=" + dep.gov().word(), acceptNew), 1, 1);
				if (i == 0) {
					fv.adjustOrPutValue(fdict.addString("AFUN1b=" + dep.reln(), acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGOV1b=" + dep.gov().word(), acceptNew), 1, 1);
				}
				// If proposition is syntactic parent of answer in any of the k-best parses
				if (dep.gov().index() == propId + 1) {
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=True", acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PisPr(A)=" + dep.reln(), acceptNew), 1, 1);
				}
			}
		}
		
		// ****************** Argument context ************************
		
		// TODO: leftmost and rightmost children of answer (word and pos)
		// TODO: left and right siblings of answer (word and pos)
		
		// left most and right most word and pos of span
		for (int[] span : sample.answerSpans) {
			if (span[0] <= answerId && answerId < span[1]) {
				fv.adjustOrPutValue(fdict.addString("ASpanLTOK=" + tokens[span[0]], acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ASpanRTOK=" + tokens[span[1]-1], acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ASpanLPOS=" + sample.postags[span[0]], acceptNew), 1, 1);
				fv.adjustOrPutValue(fdict.addString("ASpanRPOS=" + sample.postags[span[1]-1], acceptNew), 1, 1);
				break;
			}
		}
		
		// **************** Proposition-Argument relation *******************
		// Relative position
		fv.adjustOrPutValue(fdict.addString((answerId < propId ? "A<P=True" : "A>P=True"), acceptNew), 1, 1);
		
		// TODO: syntactic path between proposition and argument
		
		
		// *************** Question *******************
		// Question word and label
		fv.adjustOrPutValue(fdict.addString("WH=" + sample.qa.questionWords[0].toLowerCase(), acceptNew), 1, 1);
		fv.adjustOrPutValue(fdict.addString("QLabel=" + sample.qa.getQuestionLabel(), acceptNew), 1, 1);
		
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
