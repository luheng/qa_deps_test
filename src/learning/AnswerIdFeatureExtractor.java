package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import util.FeatureUtils;
import data.Corpus;
import data.CountDictionary;
import edu.stanford.nlp.trees.TypedDependency;

public class AnswerIdFeatureExtractor extends QAFeatureExtractor {
	
	public AnswerIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq) {
		super(corpus, numBestParses, minFeatureFreq);
	}
	
	public AnswerIdFeatureExtractor(Corpus corpus, int numBestParses,
			int minFeatureFreq, boolean useLexicalFeatures,
			boolean useDependencyFeatures, boolean use1BestFeatures) {
		super(corpus, numBestParses, minFeatureFreq, useLexicalFeatures,
				useDependencyFeatures, use1BestFeatures);
	}
	
	/*
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
	*/
	
	protected TIntDoubleHashMap extractFeatures(CountDictionary fdict,
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
		for (TypedDependency dep : FeatureUtils
				.lookupChildrenByParent(sample.kBestParses.get(0), propId)) {
			if (dep.reln().toString().equals("auxpass")) {
				pvoice = "Psv";
				break;
			}
		}
		String pvFeat = "PV=" + pvoice;
		String relPos = (answerId < propId ? "left" : "right");
		
		HashSet<String> qfeats = getQLabelFeatures(sample.questionLabel);
	
		fv.adjustOrPutValue(fdict.addString(aposFeat, acceptNew), 1, 1);
		if (useLexicalFeatures) {
			fv.adjustOrPutValue(fdict.addString(atokFeat, acceptNew), 1, 1);
		}
		
		// Proposition word and lemma, conditioned question and question label
		// Argument word and pos, conditioned on question word and label
		for (String qfeat : qfeats) {
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
			for (TypedDependency dep : FeatureUtils.lookupParentsByChild(deps, propId)) {
				String relStr = dep.reln().toString();
				String govPos = dep.gov().index() <= 0 ? "ROOT" : postags[dep.gov().index() - 1];
				String govTok = dep.gov().word();
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("PFUNkb=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("PGPoskb=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					if (i == 0 && use1BestFeatures) {
						fv.adjustOrPutValue(fdict.addString("PFUN1b=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("PGPos1b=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					}
					if (useLexicalFeatures) {
						fv.adjustOrPutValue(fdict.addString("PGTokkb=" + govTok + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						if (i == 0 && use1BestFeatures) {
							fv.adjustOrPutValue(fdict.addString("PGTok1b=" + govTok + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						}
					}
				}
			}

			// ****************** Argument syntactic context ************************
			for (TypedDependency dep : FeatureUtils.lookupParentsByChild(deps, answerId)) {
				String relStr = dep.reln().toString();
				String govPos = dep.gov().index() <= 0 ? "ROOT" : postags[dep.gov().index() - 1];
				String govTok = dep.gov().word();
				for (String qfeat : qfeats) {
					fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr + "_" + qfeat, acceptNew), 1, 1);
					// fv.adjustOrPutValue(fdict.addString("AFUNkb=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					fv.adjustOrPutValue(fdict.addString("AGPoskb=" + govPos + "_" + qfeat, acceptNew), 1, 1);
					// fv.adjustOrPutValue(fdict.addString("AGPoskb=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
					if (i == 0 && use1BestFeatures) {
						fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr + "_" + qfeat, acceptNew), 1, 1);
						// fv.adjustOrPutValue(fdict.addString("AFUN1b=" + relStr + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
						fv.adjustOrPutValue(fdict.addString("AGPos1b=" + govPos + "_" + qfeat, acceptNew), 1, 1);
						// fv.adjustOrPutValue(fdict.addString("AGPos1b=" + govPos + "_" + qfeat + "_" + aposFeat, acceptNew), 1, 1);
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
			for (TypedDependency dep : FeatureUtils.lookupChildrenByParent(deps, answerId)) {
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
			ArrayList<TypedDependency> depPath = FeatureUtils.lookupDepPath(deps, answerId, propId);
			String rels = FeatureUtils.getRelPathString(depPath, answerId);
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
			featureFreq.adjustOrPutValue(fid, 1, 1);
		}
		return fv;
	}
}
