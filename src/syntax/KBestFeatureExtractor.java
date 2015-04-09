package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import data.CountDictionary;
import data.SRLCorpus;
import data.SRLSentence;
import data.VerbInflectionDictionary;
import edu.stanford.nlp.trees.TypedDependency;
import experiments.ExperimentUtils;

public class KBestFeatureExtractor {

	private SRLCorpus corpus = null;
	private VerbInflectionDictionary inflDict = null;
	public CountDictionary featureDict = null;
	public final int minFeatureFreq;
	
	public KBestFeatureExtractor(SRLCorpus corpus, int minFeatureFreq) {
		this.corpus = corpus;
		this.minFeatureFreq = minFeatureFreq;
		featureDict = new CountDictionary();
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
	
	private String getToken(SRLSentence sent, int id) {
		return id < 0 ? "ROOT" : sent.getTokenString(id);
	}
	
	private TIntDoubleHashMap extractFeatures(QASample sample, boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		SRLSentence sent = sample.sentence;
		String[] tokens = sent.getTokensString().split("\\s+");
		
		// Proposition features
		int propId = sample.propHead;
		String prop = sent.getTokenString(propId);
		fv.adjustOrPutValue(featureDict.addString("PTok=" + prop, acceptNew), 1, 1);
		fv.adjustOrPutValue(featureDict.addString("PLem=" + inflDict.getBestBaseVerb(prop), acceptNew), 1, 1);
		for (Collection<TypedDependency> deps : sample.kBestParses) {
			for (TypedDependency dep : lookupParentsByChild(deps, propId)) {
				fv.adjustOrPutValue(featureDict.addString("PFun=" + dep.reln(), acceptNew), 1, 1);
				fv.adjustOrPutValue(featureDict.addString("PPar=" + dep.gov().word(), acceptNew), 1, 1);
			}
		
		}
		// Argument features
		for (int i = 0; i < sample.kBestParses.size(); i++) {
			Collection<TypedDependency> deps = sample.kBestParses.get(i);
			for (TypedDependency dep : deps) {
				if (dep.dep().index() == sample.answerHead + 1) {
					int gidx = dep.gov().index();
					featureDict.addString("AFUNkb=" + dep.reln());
					featureDict.addString((gidx == 0 ? "AGOVkb=ROOT" : "AGOVkb=" + tokens[gidx - 1]), acceptNew);
					if (i == 0) {
						featureDict.addString("AFUN1b=" + dep.reln(), acceptNew);
						featureDict.addString((gidx == 0 ? "AGOV1b=ROOT" : "AGOV1b=" + tokens[gidx - 1]), acceptNew);
					}
				}
			}
		}
		
		// TODO: get answer pos-tag
		//featureDict.addString("APos=" )
		
		
		// Context words
		if (sample.answerHead == 0) {
			featureDict.addString("A-1Tok=<S0>", true);
		} else {
			featureDict.addString("A-1Tok=" + sent.getTokenString(sample.answerHead - 1), true);
		}
		if (sample.answerHead == sent.length - 1) {
			featureDict.addString("A-1Tok=<SN>", true);
		} else {
			featureDict.addString("A-1Tok=" + sent.getTokenString(sample.answerHead + 1), true);
		}
		
		return fv;
	}
	
	public void extractFeatures(ArrayList<QASample> samples) {
		for (QASample sample : samples) {
			SRLSentence sent = sample.sentence;
			// Word identities
			
			
			
			// Syntax 
			
		}
		// TODO: filter low-frequency features
	}
	
	public TIntDoubleHashMap getFeatures(QASample sample) {
		TIntDoubleHashMap fv = null;
		return null;
	}
	
	
	public int numFeatures() {
		return featureDict.size();
	}
}
