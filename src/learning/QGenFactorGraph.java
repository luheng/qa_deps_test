package learning;

public class QGenFactorGraph {
	private static final int maxCliqueSize = 10000;
	
	QGenPotentialFunction potentialFunction;
	
	public double[][] cliqueScore;
	public double[][] cliqueMarginal;
	public double[][] stateMarginal;
	public double[][][] alpha, beta, best;
	public int[][][] prev;
	public int[] decode;
	public double logNorm;
	private double[] dpTemplate;
	private int sequenceLength;
	
	public QGenFactorGraph(QGenSequence sequence,
			QGenPotentialFunction potentialFunction) {
		sequenceLength = QGenSlots.numSlots;
		cliqueScore = new double[sequenceLength][];
		cliqueMarginal = new double[sequenceLength][];
		stateMarginal = new double[sequenceLength][];
		int[] csizes = potentialFunction.cliqueSizes;
		int[] lsizes = potentialFunction.latticeSizes;
		for (int i = 0; i < sequenceLength; i++) {
			cliqueScore[i] = new double[csizes[i]];
			cliqueMarginal[i] = new double[csizes[i]];
			stateMarginal[i] = new double[lsizes[i]];
		}
		dpTemplate = new double[maxCliqueSize];
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
		for (int s : fiter.states(sequenceID, 0)) { 
			alpha[0][s][S0] = edgeScore[0][s][S0][S00] + nodeScore[0][s];
		}
		int len;
		for (int i = 1; i <= length; i++) {
			for (int s : fiter.states(sequenceID, i)) {
				for (int sp : fiter.states(sequenceID, i-1)) {
					len = 0;
					for (int spp : fiter.states(sequenceID, i-2)) {
						dpTemplate[len++] = alpha[i-1][sp][spp] +
								edgeScore[i][s][sp][spp] + nodeScore[i][s];
					}
					alpha[i][s][sp] = LatticeHelper.logsum(dpTemplate, len);
				}
			}
		}
		len = 0;
		for (int sp : fiter.states(sequenceID, length-1)) {
			beta[length][SN][sp] = 0;
			dpTemplate[len++] = alpha[length][SN][sp];
		}
		logNorm = LatticeHelper.logsum(dpTemplate, len);

		for (int i = length; i > 0; i--) { 
			for (int sp : fiter.states(sequenceID, i - 1)) { 
				for (int spp : fiter.states(sequenceID, i - 2)) {
					len = 0;
					for (int s : fiter.states(sequenceID, i)) {
						dpTemplate[len++] = beta[i][s][sp] +
								edgeScore[i][s][sp][spp] + nodeScore[i][s];
					}
					beta[i-1][sp][spp] = LatticeHelper.logsum(dpTemplate, len);
				}
			}
		}
		for (int s : fiter.states(sequenceID, 0)) {
			nodeMarginal[0][s] = edgeMarginal[0][s][S0][S00] =
					alpha[0][s][S0] + beta[0][s][S0] - logNorm;
		}
		for (int i = 1; i <= length; i++) {
			for (int s : fiter.states(sequenceID, i)) {
				len = 0;
				for (int sp : fiter.states(sequenceID, i - 1)) { 
					for (int spp : fiter.states(sequenceID, i - 2)) {
						edgeMarginal[i][s][sp][spp] = alpha[i-1][sp][spp] +
								beta[i][s][sp] + edgeScore[i][s][sp][spp] +
								nodeScore[i][s] - logNorm;
					}
					dpTemplate[len++] = alpha[i][s][sp] + beta[i][s][sp] -
							logNorm;
				}
				nodeMarginal[i][s] = LatticeHelper.logsum(dpTemplate, len);
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
	
	public void addToExpectation(QGenSequence sequence, double[] empirical,
			double multiplier) {
		int[] csizes = potentialFunction.cliqueSizes;
		for (int i = 0; i < sequenceLength; i++) {
			for (int c = 0; c < csizes[i]; c++) {
				if (Double.isInfinite(cliqueMarginal[i][c])) {
					continue;
				}
				double marginal = Math.exp(cliqueMarginal[i][c]) * multiplier;
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