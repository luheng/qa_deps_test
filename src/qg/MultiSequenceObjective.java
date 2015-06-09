package qg;

import java.util.ArrayList;

import optimization.gradientBasedMethods.Objective;
import util.LatticeHelper;

public class MultiSequenceObjective extends Objective {
	QGenFactorGraph model;
	ArrayList<MultiSequence> sequences;
	protected double objective, labelLikelihood, parameterRegularizer;
	int nrFeatures;
	int numThetaUpdates;
	double gpSquared;
	double[] gradientInit;
	
	public MultiSequenceObjective(
			QGenFactorGraph model,
			ArrayList<MultiSequence> sequences,
			double[] parameters,
			double gaussianPrior) {
		this.model = model;
		this.sequences = sequences;
		this.nrFeatures = parameters.length;
		this.gradient = new double[nrFeatures];		
		this.parameters = parameters;
		this.gpSquared = gaussianPrior * gaussianPrior;
		this.numThetaUpdates = 0;
		setParameters(parameters);
	}
	
	public void updateObjectiveAndGradient() { 
		parameterRegularizer = twoNormSquared(parameters) / (2.0 * gpSquared); 
		labelLikelihood = 0;
		for (int i = 0; i < gradient.length; i++) {
			gradient[i] = parameters[i] / gpSquared;
		}
		for (int seq = 0; seq < sequences.size(); seq++) {
			MultiSequence sequence = sequences.get(seq);
			if (!sequence.isLabeled) {
				continue;
			}
			model.computeScores(seq, parameters, 0);
			model.computeMarginals();
			model.addToExpectation(seq, gradient);
			// minus labeled sum
			int numSeqs = sequence.numSequences();
			double[] seqScores = new double[numSeqs];
			for (int k = 0; k < numSeqs; k++) {
				seqScores[k] = model.computeLogLikelihood(seq,
						sequence.cliqueIds.get(k));
			}
			double logSum = LatticeHelper.logsum(seqScores, numSeqs);
			for (int k = 0; k < numSeqs; k++) {
				double weight = Math.exp(seqScores[k] - logSum);
				model.addToEmpirical(seq, sequence.cliqueIds.get(k), gradient,
						-weight);
			}
			labelLikelihood += model.logNorm - logSum;
		}
		objective = parameterRegularizer + labelLikelihood;
		//if (updateCalls % 10 == 0) {
			System.out.println("iteration:: " + updateCalls);
			System.out.println("objective:: " + objective + "\tlabeled:: " +
					labelLikelihood);
			System.out.println("gradient norm:: " + twoNormSquared(gradient));
			System.out.println("parameter norm:: " + twoNormSquared(parameters));
		//}
	}
	
	@Override
	public double getValue() {
		functionCalls++;
		return objective;
	}
	
	@Override
	public double[] getGradient() {
		gradientCalls++;
		return gradient;
	}		

	@Override
	public void setParameters(double[] newParameters) {
		super.setParameters(newParameters);
		updateObjectiveAndGradient();
	}

	@Override
	public String toString() {
		return "question generation crf objective";
	}
	
	private double twoNormSquared(double[] x) {
		double norm = .0;
		for(double v : x) {
			norm += v * v;
		}
		return norm;
	}
	
}
