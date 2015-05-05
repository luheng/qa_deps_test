package learning;

import util.LatticeHelper;

public class QGenFactorGraph {
	private static final int maxCliqueSize = 1000;
	private QGenPotentialFunction potentialFunction;
	
	public double[][] cliqueScores;
	public double[][] cliqueMarginals;
	public double[][] stateMarginals;
	public double[][] alpha, beta, best;
	public int[][][] prev;
	public int[] decode;
	public double logNorm;
	private double[] dp;
	private int sequenceLength;
	
	public QGenFactorGraph(QGenPotentialFunction potentialFunction) {
		this.potentialFunction = potentialFunction;
		sequenceLength = QGenSlots.numSlots;
		cliqueScores = new double[sequenceLength][];
		cliqueMarginals = new double[sequenceLength][];
		stateMarginals = new double[sequenceLength][];
		alpha = new double[sequenceLength][];
		beta = new double[sequenceLength][];
		int[] csizes = potentialFunction.cliqueSizes;
		int[] lsizes = potentialFunction.latticeSizes;
		for (int i = 0; i < sequenceLength; i++) {
			cliqueScores[i] = new double[csizes[i]];
			cliqueMarginals[i] = new double[csizes[i]];
			alpha[i] = new double[csizes[i]];
			beta[i] = new double[csizes[i]];
			stateMarginals[i] = new double[lsizes[i]];
		}
		dp = new double[maxCliqueSize];
		decode = new int[sequenceLength];
	}
	
	public void computeScores(QGenSequence sequence, double[] parameters,
			double smoothing) {
		for (int i = 0; i < sequenceLength; i++) {
			cliqueScores[i][sequence.cliqueIds[i]] =
					smoothing + potentialFunction.computeCliqueScore(
							sequence.sequenceId, i, sequence.latticeIds,
							parameters);
		}
	}
	
	public void computeMarginals() {
		int[][] iterator = potentialFunction.iterator;
		for (int s = 0; s < iterator[0][0]; s++) {
			int cliqueId = potentialFunction.getCliqueId(0, s, 0, 0);
			alpha[0][s] = cliqueScores[0][cliqueId];
		}
		int len = 0;
		for (int i = 1; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) { 
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					len = 0;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						dp[len++] = alpha[i-1][cLeft] + cliqueScores[i][c];
					}
					int cRight = sp * iterator[i][0] + s;
					alpha[i][cRight] = LatticeHelper.logsum(dp, len);
				}
			}
		}
		len = 0;
		int N = sequenceLength - 1;
		for (int sp = 0; sp < iterator[N][1]; sp++) {
			for (int spp = 0; spp < iterator[N][2]; spp++) {
				int cLeft = spp * iterator[N][1] + sp;
				beta[N][cLeft] = 0;
				dp[len++] = alpha[N][cLeft];
			}
		}
		logNorm = LatticeHelper.logsum(dp, len);

		for (int i = sequenceLength - 1; i > 0; i--) {
			for (int spp = 0; spp < iterator[i][2]; spp++) {
				int c0 = spp * iterator[i][0] * iterator[i][1];
				for (int sp = 0; sp < iterator[i][1]; sp++) {				
					len = 0;
					for (int s = 0; s < iterator[i][0]; s++) {
						int cRight = sp * iterator[i][0] + s;
						int c = c0 + cRight;
						dp[len++] = beta[i][cRight] + cliqueScores[i][c];
					}
					int cLeft = spp * iterator[i][1] + sp;
					beta[i-1][cLeft] = LatticeHelper.logsum(dp, len);
				}
			}
		}
		for (int i = 0; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) {
				len = 0;
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					int cRight = sp * iterator[i][0] + s;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						cliqueMarginals[i][c] =
								(i > 0 ? alpha[i-1][cLeft] : 0) +
								beta[i][cRight] + cliqueScores[i][c] - logNorm;
					}
					dp[len++] = alpha[i][cRight] + cliqueScores[i][cRight];
				}
				stateMarginals[i][s] = LatticeHelper.logsum(dp, len);
			}
		}
	}
	
	public void addToEmpirical(QGenSequence sequence, int[] gold,
			double[] empirical) {
		for (int i = 0; i < sequenceLength; i++) {
			potentialFunction.addToEmpirical(sequence.sequenceId, i, gold,
					empirical, 1.0);
		}
	}
	
	public void addToExpectation(QGenSequence sequence, double[] empirical) {
		int[] csizes = potentialFunction.cliqueSizes;
		for (int i = 0; i < sequenceLength; i++) {
			for (int c = 0; c < csizes[i]; c++) {
				if (Double.isInfinite(cliqueMarginals[i][c])) {
					continue;
				}
				double marginal = Math.exp(cliqueMarginals[i][c]);
				potentialFunction.addToEmpirical(
					sequence.sequenceId, i, c, empirical, marginal);
			}
		}
	}
	
	private void decodePosterior() {
		for (int i = 0; i < sequenceLength; i++) {
			decode[i] = 0;
			double maxq = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < potentialFunction.latticeSizes[i]; j++) {
				double q = stateMarginals[i][j];
				if (q > maxq) {
					decode[i] = j;
					maxq = q;
				}
			}
		}
	}
	
	// TODO: decode viterbi
	
	public double decodeAndEvaluate(int[] gold) {
		decodePosterior();
		double accuracy = 0;
		for (int i = 0; i < sequenceLength; i++) { 
			if (gold[i] == decode[i]) ++ accuracy;
		}
		return accuracy;
	}
}