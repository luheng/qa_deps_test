package syntax;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;

import data.CountDictionary;
import data.SRLSentence;

public class KBestFeatureExtractor {

	public CountDictionary featureDict;
	public final int minFeatureFreq;
	
	public KBestFeatureExtractor(int minFeatureFreq) {
		this.minFeatureFreq = minFeatureFreq;
		featureDict = new CountDictionary();
	}
	
	public void extractFeatures(ArrayList<QASample> samples) {
		for (QASample sample : samples) {
			SRLSentence sent = sample.sentence;
			// Word identities
			featureDict.addString("ATok=" + sent.getTokenString(sample.answerHead), true);
			featureDict.addString("PTok=" + sent.getTokenString(sample.propHead), true);
			
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
