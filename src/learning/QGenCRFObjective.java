package learning;

import java.util.ArrayList;

import optimization.gradientBasedMethods.Objective;

public class QGenCRFObjective extends Objective {
	QGenFactorGraph model;
	ArrayList<QGenSequence> sequences;
	protected double objective, labelLikelihood, parameterRegularizer;
	int nrFeatures;
	int numThetaUpdates;
	double gpSquared;
	double[] empiricalCounts, gradientInit;
	
	public QGenCRFObjective(
			QGenFactorGraph model,
			ArrayList<QGenSequence> sequences,
			double[] parameters,
			double[] empiricalCounts, double gaussianPrior) {
		this.model = model;
		this.sequences = sequences;
		this.nrFeatures = parameters.length;
		this.gradient = new double[nrFeatures];		
		this.parameters = parameters;
		this.empiricalCounts = empiricalCounts;
		this.gpSquared = gaussianPrior * gaussianPrior;
		this.gradientInit = new double[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			gradientInit[i] = - empiricalCounts[i];
		}
		this.numThetaUpdates = 0;
		setParameters(parameters);
	}
	
	public void updateObjectiveAndGradient() { 
		parameterRegularizer = twoNormSquared(parameters) / (2.0 * gpSquared); 
		labelLikelihood = 0;
		for (int i = 0; i < gradient.length; i++) {
			gradient[i] = gradientInit[i] + parameters[i] / gpSquared;
			labelLikelihood -= parameters[i] * empiricalCounts[i];
		}
		for (int seq = 0; seq < sequences.size(); seq++) {
			QGenSequence sequence = sequences.get(seq);
			if (!sequence.isLabeled) {
				continue;
			}			
			model.computeScores(sequence, parameters, 0);
			model.computeMarginals();
			model.addToExpectation(sequence, gradient);
			labelLikelihood += model.logNorm;
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
