package learning;

import util.LatticeHelper;

public class QGenFactorGraph {
	private static final int maxCliqueSize = 10000;
	
	QGenPotentialFunction potentialFunction;
	
	public double[][] cliqueScore;
	public double[][] cliqueMarginal;
	public double[][] stateMarginal;
	public double[][] alpha, beta, best;
	public int[][][] prev;
	public int[] decode;
	public double logNorm;
	private double[] dp;
	private int sequenceLength;
	
	public QGenFactorGraph(QGenSequence sequence,
			QGenPotentialFunction potentialFunction) {
		sequenceLength = QGenSlots.numSlots;
		cliqueScore = new double[sequenceLength][];
		cliqueMarginal = new double[sequenceLength][];
		stateMarginal = new double[sequenceLength][];
		alpha = new double[sequenceLength][];
		beta = new double[sequenceLength][];
		int[] csizes = potentialFunction.cliqueSizes;
		int[] lsizes = potentialFunction.latticeSizes;
		for (int i = 0; i < sequenceLength; i++) {
			cliqueScore[i] = new double[csizes[i]];
			cliqueMarginal[i] = new double[csizes[i]];
			alpha[i] = new double[csizes[i]];
			beta[i] = new double[csizes[i]];
			stateMarginal[i] = new double[lsizes[i]];
		}
		dp = new double[maxCliqueSize];
		decode = new int[sequenceLength];
	}
	
	public void computeScores(QGenSequence sequence, double[] parameters,
			double smoothing) {
		for (int i = 0; i < sequenceLength; i++) {
			cliqueScore[i][sequence.cliqueIds[i]] =
					smoothing + potentialFunction.computeCliqueScore(
							sequence.sequenceId, i, sequence.latticeIds,
							parameters);
		}
	}
	
	public void computeMarginals() {
		int[][] iterator = potentialFunction.iterator;
		for (int s = 0; s < iterator[0][0]; s++) {
			int cliqueId = potentialFunction.getCliqueId(0, s, 0, 0);
			alpha[0][s] = cliqueScore[0][cliqueId];
		}
		int len = 0;
		for (int i = 1; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) { 
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					len = 0;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						dp[len++] = alpha[i-1][cLeft] + cliqueScore[i][c];
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
				int cOld = spp * iterator[N][1] + sp;
				beta[N][cOld] = 0;
				dp[len++] = alpha[N][cOld];
			}
		}
		logNorm = LatticeHelper.logsum(dp, len);

		for (int i = sequenceLength - 1; i > 0; i--) {
			for (int spp = 0; spp < iterator[i][2]; spp++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {				
					len = 0;
					for (int s = 0; s < iterator[i][0]; s++) {
						int cRight = sp * iterator[i][0] + s;
						int c = iterator[i][1] * spp + cRight;
						dp[len++] = beta[i][cRight] + cliqueScore[i][c];
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
						cliqueMarginal[i][c] =
								alpha[i-1][cLeft] + beta[i][cRight] +
								cliqueScore[i][c] - logNorm;
						
					}
					dp[len++] = alpha[i][cRight] + cliqueScore[i][cRight];
					stateMarginal[i][s] = LatticeHelper.logsum(dp, len);
				}
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
				if (Double.isInfinite(cliqueMarginal[i][c])) {
					continue;
				}
				double marginal = Math.exp(cliqueMarginal[i][c]);
				potentialFunction.addToEmpirical(
					sequence.sequenceId, i, sequence.latticeIds,
					empirical, marginal);
			}
		}
	}
	
	private void decodePosterior() {
		for (int i = 0; i < sequenceLength; i++) {
			decode[i] = 0;
			double maxq = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < potentialFunction.latticeSizes[i]; j++) {
				double q = stateMarginal[i][j];
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