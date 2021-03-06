package qg;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import learning.FeatureVector;
import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotPrepositions;
import annotation.QASlotQuestionWords;
import annotation.QASlots;

public class QGenPotentialFunction {
	public static final int kSequenceOrder = 3;
	
	public String[][] lattice;
	public int[] latticeSizes, cliqueSizes;
	public int[][] iterator;  // slot-id, offset

	private int seqLength;
	public FeatureVector[][] transitionFeatures; // slot-id, clique-id
	public FeatureVector[][][] emissionFeatures; // seq-id, slot-id, state-di
	
	public QGenPotentialFunction() {
		initializeLattice();
	}
	
	private void initializeLattice() {
		seqLength = QASlots.numSlots;
		lattice = new String[seqLength][];
		// 1. WH
		lattice[0] = Arrays.copyOf(QASlotQuestionWords.values,
								   QASlotQuestionWords.values.length);
		// 2. AUX
		lattice[1] = Arrays.copyOf(QASlotAuxiliaryVerbs.values,
				   				   QASlotAuxiliaryVerbs.values.length);
		// 3. PH1
		lattice[2] = Arrays.copyOf(QASlotPlaceHolders.values,
								   QASlotPlaceHolders.values.length);
		// 4. TRG
		ArrayList<String> trgOptions = getGenericTrgOptions();
		lattice[3] = trgOptions.toArray(new String[trgOptions.size()]);
		// 5. PH2
		lattice[4] = Arrays.copyOf(QASlotPlaceHolders.values,
								   QASlotPlaceHolders.values.length);
		// 6. PP
		lattice[5] = new String[QASlotPrepositions.values.length + 1];
		lattice[5][0] = "";
		for (int i = 0; i < QASlotPrepositions.values.length; i++) {
			lattice[5][i+1] = QASlotPrepositions.values[i];
		}
		// 7. PH3
		lattice[6] = Arrays.copyOf(QASlotPlaceHolders.ph3Values,
								   QASlotPlaceHolders.ph3Values.length);
		
		long latSize = 1;
		
		for (int i = 0; i < 7; i++) {
			for (String lat : lattice[i]) {
				System.out.println(lat);
			}
			System.out.println();
			latSize *= lattice[i].length;
		}
		System.out.println(latSize);
		latticeSizes = new int[seqLength];
		cliqueSizes = new int[seqLength];
		iterator = new int[seqLength][kSequenceOrder];
		for (int i = 0; i < seqLength; i++) {
			cliqueSizes[i] = 1;
			latticeSizes[i] = lattice[i].length;
			for (int j = 0; j < kSequenceOrder; j++) {
				int k = i - j;
				int lsize = (k < 0 ? 1 : latticeSizes[k]);
				iterator[i][j] = lsize;
				cliqueSizes[i] *=  lsize;
			}
		}
	}
	
	private static ArrayList<String> getGenericTrgOptions() {
		HashSet<String> opSet = new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		String[] inflections = new String[] {
				"do", "does", "doing", "did", "done"};
		for (int i = 0; i < inflections.length; i++) {
			opSet.add(inflections[i]);
		}
		opSet.add("be " + inflections[4]);
 		opSet.add("been " + inflections[4]);
		opSet.add("have " + inflections[4]);
		opSet.add("have been " + inflections[4]);
		opSet.add("being " + inflections[4]);
		opSet.add("be " + inflections[2]);
		opSet.add("been " + inflections[2]);
		opSet.add("have been " + inflections[2]);
		for (String op : opSet) {
			options.add(op);
		}
		Collections.sort(options);
		return options;
	}
	
	public void extractMultiSequenceFeatures(
			ArrayList<MultiSequence> sequences,
			QGenFeatureExtractor featureExtractor) {
		// Extract transition features.
		int numSequences = sequences.size();
		transitionFeatures = new FeatureVector[seqLength][];
		emissionFeatures = new FeatureVector[numSequences][seqLength][];
		
		// Pre-compute emission features
		for (MultiSequence sequence : sequences) {
			if (!sequence.isLabeled) {
				continue;
			}
			for (int k = 0; k < sequence.numSequences(); k++) {
				for (int i = 0; i < seqLength; i++) {
					for (int s = 0; s < iterator[i][0]; s++) {
						featureExtractor.extractEmissionFeatures(
								sequence.sentence, sequence.samples.get(k),
								lattice, i, s, true /* accept new */);
					}
				}
			}
		}
		System.out.println(String.format("Extracted %d emission features.",
				featureExtractor.featureDict.size()));
		featureExtractor.pruneFeatures();
		System.out.println(String.format("%d features left after pruning.",
				featureExtractor.featureDict.size()));

		// Extract transition features.
		for (int i = 0; i < seqLength; i++) {
			transitionFeatures[i] = new FeatureVector[cliqueSizes[i]];
			for (int s = 0; s < iterator[i][0]; s++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cliqueId = getCliqueId(i, s, sp, spp);
						TIntDoubleHashMap fv =
							featureExtractor.extractTransitionFeatures(
								lattice, i, s, sp, spp, true /* accept new */);
						transitionFeatures[i][cliqueId] = new FeatureVector(fv);
					}
				}
			}
		}		
		// Extract emission features.
		for (int seq = 0; seq < numSequences; seq++) {
			MultiSequence sequence = sequences.get(seq);
			for (int k = 0; k < sequence.numSequences(); k++) {
				for (int i = 0; i < seqLength; i++) {
					emissionFeatures[seq][i] = new FeatureVector[latticeSizes[i]];
					for (int s = 0; s < iterator[i][0]; s++) {
						TIntDoubleHashMap fv =
							featureExtractor.extractEmissionFeatures(
								sequence.sentence, sequence.samples.get(k),
								lattice, i, s, false /* accept new */);
						emissionFeatures[seq][i][s] = new FeatureVector(fv);
					}
				}
			}
		}
		int numFeatures = featureExtractor.featureDict.size();
		System.out.println(String.format("Extracted %d features.", numFeatures));
	}
	
	public void extractFeatures(
			ArrayList<QGenSequence> sequences,
			QGenFeatureExtractor featureExtractor) {
		// Extract transition features.
		int numSequences = sequences.size();
		transitionFeatures = new FeatureVector[seqLength][];
		emissionFeatures = new FeatureVector[numSequences][seqLength][];
		
		// Pre-compute emission features
		for (int seq = 0; seq < numSequences; seq++) {
			QGenSequence sequence = sequences.get(seq);
			if (!sequence.isLabeled) {
				continue;
			}
			for (int i = 0; i < seqLength; i++) {
				for (int s = 0; s < iterator[i][0]; s++) {
					featureExtractor.extractEmissionFeatures(
							sequence.sentence, sequence.sample, lattice, i, s,
							true /* accept new */);
				}
			}
		}
		System.out.println(String.format("Extracted %d emission features.",
				featureExtractor.featureDict.size()));
		featureExtractor.pruneFeatures();
		System.out.println(String.format("%d features left after pruning.",
				featureExtractor.featureDict.size()));

		// Extract transition features.
		for (int i = 0; i < seqLength; i++) {
			transitionFeatures[i] = new FeatureVector[cliqueSizes[i]];
			for (int s = 0; s < iterator[i][0]; s++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cliqueId = getCliqueId(i, s, sp, spp);
						TIntDoubleHashMap fv =
							featureExtractor.extractTransitionFeatures(
								lattice, i, s, sp, spp, true /* accept new */);
						transitionFeatures[i][cliqueId] = new FeatureVector(fv);
					}
				}
			}
		}		
		// Extract emission features.
		for (int seq = 0; seq < numSequences; seq++) {
			QGenSequence sequence = sequences.get(seq);
			for (int i = 0; i < seqLength; i++) {
				emissionFeatures[seq][i] = new FeatureVector[latticeSizes[i]];
				for (int s = 0; s < iterator[i][0]; s++) {
					TIntDoubleHashMap fv =
						featureExtractor.extractEmissionFeatures(
							sequence.sentence, sequence.sample, lattice, i, s,
							false /* accept new */);
					emissionFeatures[seq][i][s] = new FeatureVector(fv);
				}
			}
		}
		int numFeatures = featureExtractor.featureDict.size();
		System.out.println(String.format("Extracted %d features.", numFeatures));
	}
		
	/**
	 * For example, [0, 0, 5] , [1, 1, 10] -> (1 * 10 * 0) + (10 * 0) + 5
	 * For example, [2, 3, 5] , [3, 5, 10] -> (5 * 10 * 2) + (10 * 3) + 5
	 */
	public int getCliqueId(int slotId, int[] latticeIds) {
		/*
		int step = 1, cliqueId = 0;
		for (int i = slotId; i > slotId - kSequenceOrder; i--) {
			cliqueId += step * (i < 0 ? 0 : latticeIds[i]);
			step *= (i < 0 ? 1 : latticeSizes[i]);
		}
		*/
		int s = latticeIds[slotId];
		int sp = (slotId > 0 ? latticeIds[slotId - 1] : 0);
		int spp = (slotId > 1 ? latticeIds[slotId - 2] : 0);
		return getCliqueId(slotId, s, sp, spp);
	}
	
	public int getCliqueId(int slotId, int s, int sp, int spp) {
		return s + sp * iterator[slotId][0] +
			spp * iterator[slotId][0] * iterator[slotId][1];
	}
	
	/**
	 * 
	 * @param slotId
	 * @param cliqueId
	 * @return int[] {s, sp, spp}
	 */
	public int[] getStateIds(int slotId, int cliqueId) {
		int c = cliqueId;
		int s = c % iterator[slotId][0];
		c /= iterator[slotId][0];
		int sp = c % iterator[slotId][1];
		int spp = c / iterator[slotId][1];
		return new int[] {s, sp, spp};
	}
	
	public double computeCliqueScore(int seq, int slot, int clique,
			double[] parameters) {
		int state = clique % iterator[slot][0];
		double score = .0;
		FeatureVector tf = transitionFeatures[slot][clique],
				      ef = emissionFeatures[seq][slot][state];
		for (int i = 0; i < tf.length; i++) {
			score += tf.vals[i] * parameters[tf.ids[i]];
		}
		for (int i = 0; i < ef.length; i++) {
			score += ef.vals[i] * parameters[ef.ids[i]];
		}
		return score;
	}
	
	/*
	public void addToEmpirical(int seq, int slot, int[] cliqueIds,
			double[] empirical, double marginal) {	
		if (marginal == 0 || Double.isInfinite(marginal) ||
				Double.isNaN(marginal)) {
			return;
		}
		int stateId = cliqueIds[slot] % iterator[slot][0];
		FeatureVector tf = transitionFeatures[slot][cliqueIds[slot]],
			      	  ef = emissionFeatures[seq][slot][stateId];
		for (int i = 0; i < tf.length; i++) {
			empirical[tf.ids[i]] += marginal * tf.vals[i];
		}
		for (int i = 0; i < ef.length; i++) {
			empirical[ef.ids[i]] += marginal * ef.vals[i];
		}	
	}
	*/

	public void addToEmpirical(int seq, int slot, int cliqueId,
			double[] empirical, double marginal) {
		if (marginal == 0 || Double.isInfinite(marginal) ||
				Double.isNaN(marginal)) {
			return;
		}
		int state = cliqueId % iterator[slot][0];
		FeatureVector tf = transitionFeatures[slot][cliqueId],
			      	  ef = emissionFeatures[seq][slot][state];
		for (int i = 0; i < tf.length; i++) {
			empirical[tf.ids[i]] += marginal * tf.vals[i];
		}
		for (int i = 0; i < ef.length; i++) {
			empirical[ef.ids[i]] += marginal * ef.vals[i];
		}	
	}

	public boolean sanityCheck(QGenFactorGraph graph) {
		for (int i = 0; i < seqLength; i++) {
			double stateSum = 0;
			for (int j = 0; j < latticeSizes[i]; j++) {
				double marg = Math.exp(graph.stateMarginals[i][j]);
			//	System.out.print(lattice[i][j] + ":" + marg + "\t");
				stateSum += marg;
			}
			//System.out.println(stateSum);
			if (Math.abs(stateSum - 1.0) > 1e-6) {
				return false;
			}
		}
		return true;
	}
}
