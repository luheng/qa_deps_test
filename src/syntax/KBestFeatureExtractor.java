package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;

import data.CountDictionary;
import data.SRLSentence;
import data.UniversalPostagMap;
import edu.stanford.nlp.trees.TypedDependency;

public class KBestFeatureExtractor {

	public CountDictionary featureDict;
	public final int minFeatureFreq;
	
	public KBestFeatureExtractor(int minFeatureFreq) {
		this.minFeatureFreq = minFeatureFreq;
		featureDict = new CountDictionary();
	}
	
	private TIntDoubleHashMap extractFeatures(QASample sample,
			boolean acceptNew) {
		TIntDoubleHashMap fv = new TIntDoubleHashMap();
		SRLSentence sent = sample.sentence;
		String[] tokens = sent.getTokensString().split("\\s+");
		
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
		
		
		// Proposition features
		featureDict.addString("PTok=" + sent.getTokenString(sample.propHead), acceptNew);
		
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
