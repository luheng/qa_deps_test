package qg;

import java.util.ArrayList;

import annotation.QASlots;
import optimization.gradientBasedMethods.LBFGS;
import optimization.gradientBasedMethods.Optimizer;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.linesearch.WolfRuleLineSearch;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.NormalizedValueDifference;
import util.LatticeUtils;
import data.Corpus;

public class QGenCRF extends QGLearner {
	double[] parameters;
	double[] empiricalCounts;
	
	public QGenCRF(Corpus baseCorpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets) {
		super(baseCorpus, trainSet, testSets);
	}
	
	public void run() {
		LineSearchMethod lineSearch;
		CompositeStopingCriteria stopping;
		Optimizer optimizer;
		OptimizerStats stats;
		double prevStepSize = 1e-4;
		QGenCRFObjective objective;
		int numIters = 30;
		double stopThreshold = 1e-3;
		double gaussianPrior = 10;
		
		System.out.println("Start CRF training");
		
		// ******* initialize model
		numFeatures = featureExtractor.featureDict.size();
		parameters = new double[numFeatures];
		empiricalCounts = new double[numFeatures];
		for (QGenSequence sequence : sequences) {
			if (!sequence.isLabeled) {
				continue;
			}
			for (int i = 0; i < QASlots.numSlots; i++) {
				potentialFunction.addToEmpirical(sequence.sequenceId, i,
						sequence.latticeIds, empiricalCounts, 1.0);
			}
		}
		System.out.println("Empirical count l2norm:\t" +
				LatticeUtils.L2NormSquared(empiricalCounts));
		
		lineSearch = new WolfRuleLineSearch(
				new InterpolationPickFirstStep(prevStepSize), 1e-4, 0.9, 10);
		lineSearch.setDebugLevel(0);
		stopping = new CompositeStopingCriteria();
		stopping.add(new NormalizedValueDifference(stopThreshold));
		optimizer = new LBFGS(lineSearch, 10);
		optimizer.setMaxIterations(numIters);
		stats = new OptimizerStats();
		objective = new QGenCRFObjective(
				new QGenFactorGraph(potentialFunction),
				sequences, parameters, empiricalCounts, gaussianPrior); 
		boolean succeed = optimizer.optimize(objective, stats, stopping);
		prevStepSize = optimizer.getCurrentStep();
		System.out.println("success:\t" + succeed + "\twith latest stepsize:\t"
				+ prevStepSize);
	
		double obj = objective.objective;
			
		System.out.println("Negative Labeled Likelihood::\t" +
				objective.labelLikelihood);
		System.out.println("*** Combined objective::\t" + obj);
		
		predict();
	}
	
	public void predict() {
		final int topK = 10;
		for (QGenSequence seq : sequences) {
			if (seq.isLabeled) {
				continue;
			}
			QGenFactorGraph graph = new QGenFactorGraph(potentialFunction);
			graph.computeScores(seq.sequenceId, parameters, 0);
			System.out.println("*" + getQuestion(seq.sentence,
					seq.propHead, seq.latticeIds));
			int[][] kdecoded = graph.kbestViterbi(topK);
			for (int k = 0; k < topK; k++) {
				System.out.println(getQuestion(seq.sentence,
						seq.propHead, kdecoded[k]));
				//for (int i = 0; i < decoded.length; i++) {
				//	System.out.print(potentialFunction.lattice[i][decoded[k][i]] + "\t");
				//}
				//System.out.println();
			}
			System.out.println();
		}
	}
}
