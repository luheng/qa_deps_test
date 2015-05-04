package learning;

import optimization.gradientBasedMethods.Objective;

public class QGenCRFObjective extends Objective {
	
	protected double objective, labelLikelihood, parameterRegularizer;
	int nrFeatures;
	int numThetaUpdates;
	double gpSquared;
	double[] empiricalCounts, gradientInit;
	
	public QGenCRFObjective(double[] parameters,
			double[] empiricalCounts, double gaussianPrior) {
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
		for (int i = 0; i < numSequences; i++) {
			AbstractSequence instance = corpus.getInstance(sid);
			if (!instance.isLabeled) {
				continue;
			}
			model.computeScores(instance, parameters, 0.0);
			model.computeMarginals();
			
			model.addToExpectation(sid, gradient);
			labelLikelihood += model.logNorm;
		}
		objective = parameterRegularizer +  labelLikelihood;
		
		if (updateCalls % 100 == 0) {
			System.out.println("iteration:: " + updateCalls);
			System.out.println("objective:: " + objective + "\tlabeled:: " +
					labelLikelihood);
			System.out.println("gradient norm:: " + twoNormSquared(gradient));
			System.out.println("parameter norm:: " + twoNormSquared(parameters));
		}
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
		return "ocr discriminative model objective";
	}
	
	private void update() {
		
	}
	

	private double twoNormSquared(double[] x) {
		double norm = .0;
		for(double v : x) {
			norm += v * v;
		}
		return norm;
	}
	
}
