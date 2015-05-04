package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import util.StringUtils;
import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotPrepositions;
import annotation.QASlotQuestionWords;

public class QGenPotentialFunction {
	public static final int kSequenceOrder = 3;
	
	public String[][] lattice;
	public int[] latticeSizes, cliqueSizes;
	public int[][] iterator;  // slot-id, offset

	private int seqLength;
	
	public int[][][] featureIds;     // slot-id, clique-id 
	public double[][][] featureVals;
	//public int[][][][] contextFeatureIds // sent-id, slot-id,
	
	public QGenPotentialFunction() {
		initializeLattice();
	}
	
	private void initializeLattice() {
		seqLength = QGenSlots.numSlots;
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
		lattice[5] = Arrays.copyOf(QASlotPrepositions.values,
								   QASlotPrepositions.values.length);
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
	
	public void extractFeatures(QuestionIdDataset trains,
			QGenFeatureExtractor featureExtractor) {
		// Extract transition features.
		
		for (int i = 0; i < seqLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						featureExtractor.extractTransitionFeatures(
								lattice, i, s, sp, spp, true /* accept new */);
					}
				}
			}
		}
		
		//featureExtractor.freeze();
		int numFeatures = featureExtractor.featureDict.size();
		System.out.println(String.format("Extracted %d features.", numFeatures));
		
		featureIds = new int[seqLength][][];
		featureVals = new double[seqLength][][];
		for (int i = 0; i < seqLength; i++) {
			featureIds[i] = new int[cliqueSizes[i]][];
			featureVals[i] = new double[cliqueSizes[i]][];
			for (int s = 0; s < iterator[i][0]; s++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cliqueId = getCliqueId(i, s, sp, spp);
						TIntDoubleHashMap fv =
							featureExtractor.extractTransitionFeatures(
								lattice, i, s, sp, spp, false /* accept new */);
						
						int[] fids = Arrays.copyOf(fv.keys(), fv.size());
						Arrays.sort(fids);
						featureIds[i][cliqueId] = new int[fids.length];
						featureVals[i][cliqueId] = new double[fids.length];
						for (int j = 0; j < fids.length; j++) {
							featureIds[i][cliqueId][j] = fids[j];
							featureVals[i][cliqueId][j] = fv.get(fids[j]);
						}
					}
				}
			}
		}
	}
		
	/**
	 * For example, [0, 0, 5] , [1, 1, 10] -> (1 * 10 * 0) + (10 * 0) + 5
	 * For example, [2, 3, 5] , [3, 5, 10] -> (5 * 10 * 2) + (10 * 3) + 5
	 */
	public int getCliqueId(int slotId, int[] latticeIds) {		
		int step = 1, cliqueId = 0;
		for (int i = slotId; i > slotId - kSequenceOrder; i--) {
			cliqueId += step * (i < 0 ? 0 : latticeIds[i]);
			step *= (i < 0 ? 1 : latticeSizes[i]);
		}
		return cliqueId;
	}
	
	public int getCliqueId(int slotId, int s, int sp, int spp) {
		return s + sp * iterator[slotId][0] +
			spp * iterator[slotId][0] * iterator[slotId][1];
	}
	
	public double computeCliqueScore(int seq, int slot, int[] states,
			double[] parameters) {
		int clique = getCliqueId(slot, states);
		double score = .0;
		for (int i = 0; i < featureIds[slot][clique].length; i++) {
			score += featureIds[slot][clique][i] * featureVals[slot][clique][i];
		}
		return score;
	}
	
	public void addToEmpirical(int seq, int slot, int[] states,
			double[] empirical, double marginal) {	
		if (marginal == 0 || Double.isInfinite(marginal) ||
				Double.isNaN(marginal)) {
			return;
		}
		int clique = getCliqueId(slot, states);
		// System.out.println(slot + "\t" + StringUtils.intArrayToString(",", states) + "\t" + clique);
		for (int i = 0; i < featureIds[slot][clique].length; i++) {
			int fid = featureIds[slot][clique][i];
			empirical[fid] += marginal * featureVals[slot][clique][i];
		}
	}
}
