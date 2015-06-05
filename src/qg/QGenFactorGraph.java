package qg;

import java.util.Arrays;

import annotation.QASlots;
import util.LatticeHelper;

public class QGenFactorGraph {
	private static final int maxCliqueSize = 1000;
	private QGenPotentialFunction potentialFunction;
	
	public double[][] cliqueScores;
	public double[][] cliqueMarginals;
	public double[][] stateMarginals;
	public double[][] alpha, beta;
	public int[][][] prev;
	public double logNorm;
	private double[] dp;
	private int sequenceLength;
	
	public QGenFactorGraph(QGenPotentialFunction potentialFunction) {
		this.potentialFunction = potentialFunction;
		sequenceLength = QASlots.numSlots;
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
	}
	
	public void computeScores(int seqId, double[] parameters,
			double smoothing) {
		for (int i = 0; i < sequenceLength; i++) {
			for (int c = 0; c < potentialFunction.cliqueSizes[i]; c++) {
				cliqueScores[i][c] =
						smoothing + potentialFunction.computeCliqueScore(
								seqId, i, c, parameters);
			}
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
		int n = sequenceLength - 1;
		for (int s = 0; s < iterator[n][0]; s++) {
			for (int sp = 0; sp < iterator[n][1]; sp++) {
				int c = sp * iterator[n][0] + s;
				beta[n][c] = 0;
				dp[len++] = alpha[n][c];
			}
		}
		logNorm = LatticeHelper.logsum(dp, len);

		for (int i = n; i > 0; i--) {
			for (int spp = 0; spp < iterator[i][2]; spp++) {
				int c0 = spp * iterator[i][1] * iterator[i][0];
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
		for (int s = 0; s < iterator[0][0]; s++) {
			int cliqueId = potentialFunction.getCliqueId(0, s, 0, 0);
			cliqueMarginals[0][cliqueId] = alpha[0][s] + beta[0][s] - logNorm;
		}
		for (int i = 1; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) {
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					int cRight = sp * iterator[i][0] + s;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						cliqueMarginals[i][c] = alpha[i-1][cLeft] +
								beta[i][cRight] + cliqueScores[i][c] - logNorm;
					}
				}
			}
		}
		for (int i = 0; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) {
				stateMarginals[i][s] = 0;
				len = 0;
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int c = potentialFunction.getCliqueId(i, s, sp, spp);
						dp[len++] = cliqueMarginals[i][c];
					}
				}
				stateMarginals[i][s] = LatticeHelper.logsum(dp, len);
			}
		}
		if (!potentialFunction.sanityCheck(this)) {
			System.out.println("sanity check failed!");
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
	
	private int[] posterior() {
		int[] decode = new int[sequenceLength];
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
		return decode;
	}
	
	public int[] viterbi() {
		double[][] best = new double[sequenceLength][];
		int[][] backPtr = new int[sequenceLength][];
		int[] csizes = potentialFunction.cliqueSizes;
		for (int i = 0; i < sequenceLength; i++) {
			best[i] = new double[csizes[i]];
			backPtr[i] = new int[csizes[i]];
		}
		int[][] iterator = potentialFunction.iterator;
		for (int i = 0; i < sequenceLength; i++) {
			Arrays.fill(best[i], Double.NEGATIVE_INFINITY);
		}
		for (int s = 0; s < iterator[0][0]; s++) {
			int cliqueId = potentialFunction.getCliqueId(0, s, 0, 0);
			best[0][s] = cliqueScores[0][cliqueId];
		}
		for (int i = 1; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) { 
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					int cRight = sp * iterator[i][0] + s;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						double score = best[i-1][cLeft] + cliqueScores[i][c];
						if (score > best[i][cRight]) {
							best[i][cRight] = score;
							backPtr[i][cRight] = spp;
						}
					}
				}
			}
		}
		int[] decoded = new int[sequenceLength];
		int n = sequenceLength - 1, cRight = 0;
		for (int c = 1; c < backPtr[n].length; c++) {
			if (best[n][c] > best[n][cRight]) {
				cRight = c;
			}
		}
		for (int i = n; i >= 0; i--) {
			decoded[i] = cRight % iterator[i][0];
			if (i > 0) {
				int spp = backPtr[i][cRight];
				int sp = cRight / iterator[i][0];
				cRight = spp * iterator[i][1] + sp;
			}
		}
		return decoded;
	}
	
	private void kbestUpdate(double newScore, int newPtr, int newPtr2,
			double[] scores, int[] ptr, int[] ptr2) {
		int idx = 0;
		for ( ; idx < scores.length; idx++) {
			if (newScore > scores[idx]) {
				for (int j = scores.length - 1; j > idx; j--) {
					scores[j] = scores[j-1];
					ptr[j] = ptr[j-1];
					ptr2[j] = ptr2[j-1];
				}
				scores[idx] = newScore;
				ptr[idx] = newPtr;
				ptr2[idx] = newPtr2;
			}
		}
	}
	
	public int[][] kbestViterbi(int topK) {
		double[][][] best = new double[sequenceLength + 1][][];
		int[][][] backPtr = new int[sequenceLength + 1][][],
				  backPtr2 = new int[sequenceLength + 1][][];
		for (int i = 0; i <= sequenceLength; i++) {
			int cs = (i < sequenceLength ? potentialFunction.cliqueSizes[i] : 1);
			best[i] = new double[cs][topK];
			backPtr[i] = new int[cs][topK];
			backPtr2[i] = new int[cs][topK];
			for (int c = 0; c < cs; c++) {
				Arrays.fill(best[i][c], Double.NEGATIVE_INFINITY);
			}
		}		
		int[][] iterator = potentialFunction.iterator;
		for (int s = 0; s < iterator[0][0]; s++) {
			int cliqueId = potentialFunction.getCliqueId(0, s, 0, 0);
			kbestUpdate(cliqueScores[0][cliqueId], cliqueId, 0, best[0][s],
					backPtr[0][s], backPtr2[0][s]);
		}
		for (int i = 1; i < sequenceLength; i++) {
			for (int s = 0; s < iterator[i][0]; s++) { 
				for (int sp = 0; sp < iterator[i][1]; sp++) {
					int cRight = sp * iterator[i][0] + s;
					for (int spp = 0; spp < iterator[i][2]; spp++) {
						int cLeft = spp * iterator[i][1] + sp;
						int c = cLeft * iterator[i][0] + s;
						for (int k = 0; k < topK; k++) {
							double score =
									best[i-1][cLeft][k] + cliqueScores[i][c];
							kbestUpdate(score, spp, k, best[i][cRight],
									backPtr[i][cRight], backPtr2[i][cRight]);
						}
					}
				}
			}
		}
		int[][] decoded = new int[topK][sequenceLength];
		int n = sequenceLength - 1;
		for (int c = 1; c < backPtr[n].length; c++) {
			for (int k = 0; k < topK; k++) {
				kbestUpdate(best[n][c][k], c, k, best[n+1][0], backPtr[n+1][0],
						backPtr2[n+1][0]);
			}
		}
		for (int k0 = 0; k0 < topK; k0++) {
			int cRight = backPtr[n+1][0][k0], k = k0;
			for (int i = n; i >= 0; i--) {
				decoded[k][i] = cRight % iterator[i][0];
				if (i > 0) {
					int spp = backPtr[i][cRight][k];
					int sp = cRight / iterator[i][0];
					cRight = spp * iterator[i][1] + sp;
					k = backPtr2[i][cRight][k];
				}
			}
		}
		return decoded;
	}
	
	public double decodeAndEvaluate(int[] gold) {
		int[] decode = posterior();
		double accuracy = 0;
		for (int i = 0; i < sequenceLength; i++) { 
			if (gold[i] == decode[i]) ++ accuracy;
		}
		return accuracy;
	}
}