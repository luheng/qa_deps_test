package learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
	private int[][][][] featureIds;
	private double[][][][] featureVals;
	
	public QGenPotentialFunction() {
		initializeLattice();
	}
	
	public void setFeatures(int[][][][] featureIds,
			double[][][][] featureVals) {
		this.featureIds = featureIds;
		this.featureVals = featureVals;
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
		lattice[4] = Arrays.copyOf(QASlotPlaceHolders.values,
								   QASlotPlaceHolders.values.length);
		// 7. PH3
		lattice[6] = Arrays.copyOf(QASlotPrepositions.values,
								   QASlotPrepositions.values.length);
		
		//latticeSizes = new int[seqLength][kSequenceOrder];
		latticeSizes = new int[seqLength];
		cliqueSizes = new int[seqLength];
		iterator = new int[seqLength][kSequenceOrder];
		for (int i = 0; i < seqLength; i++) {
			cliqueSizes[i] = 1;
			latticeSizes[i] = lattice[i].length;
			for (int j = 0; j < kSequenceOrder; j++) {
				int k = i - kSequenceOrder + j + 1;
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
	
	/**
	 * For example, [0, 0, 5] , [1, 1, 10] -> (1 * 10 * 0) + (10 * 0) + 5
	 * For example, [2, 3, 5] , [3, 5, 10] -> (5 * 10 * 2) + (10 * 3) + 5
	 * @param latticeIds
	 * @param latticeSizes
	 * @return
	 */
	@Deprecated
	public int getCliqueId(int[] latticeIds) {
		int step = 1, cliqueId = 0;
		for (int i = latticeIds.length - 1; i >= 0; i--) {
			cliqueId += step * latticeIds[i];
			step *= latticeSizes[i];
		}
		return cliqueId;
	}
	
	public int getCliqueId(int slotId, int[] latticeIds) {
		int step = 1, cliqueId = 0;
		for (int i = slotId; i > slotId - kSequenceOrder; i--) {
			cliqueId += step * (i < 0 ? 1 : latticeIds[i]);
			step *= (i < 0 ? 1 : latticeSizes[i]);
		}
		return cliqueId;
	}
	
	public int getCliqueId(int slotId, int s, int sp, int spp) {
		return s +
			   sp * iterator[slotId][0] +
			   spp * iterator[slotId][0] * iterator[slotId][1];
	}
	
	@Deprecated
	public int[] getLatticeIds(int cliqueId) {
		int step = 1, rem = cliqueId;
		int[] latticeIds = new int[latticeSizes.length];
		for (int i = 1; i < latticeSizes.length; i++) {
			step *= latticeSizes[i];
		}
		for (int i = 0; i < latticeSizes.length; i++) {
			latticeIds[i] = rem / step;
			rem %= step;
			if (i + 1 < latticeSizes.length) {
				step /= latticeSizes[i + 1];
			}
		}
		return latticeIds;
	}

	public double computeCliqueScore(int seq, int slot, int[] states,
			double[] parameters) {
		int clique = getCliqueId(slot, states);
		double score = .0;
		for (int i = 0; i < featureIds[seq][slot][clique].length; i++) {
			score += featureIds[seq][slot][clique][i] *
					 featureVals[seq][slot][clique][i];
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
		for (int i = 0; i < featureIds[seq][slot][clique].length; i++) {
			int fid = featureIds[seq][slot][clique][i];
			empirical[fid] += marginal * featureVals[seq][slot][clique][i];
		}
	}
}
