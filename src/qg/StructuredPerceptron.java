package qg;

import java.util.ArrayList;
import java.util.Arrays;

import config.QuestionIdConfig;
import util.LatticeUtils;
import data.Corpus;

public class StructuredPerceptron extends QGLearner {
	public double[] weights, avgWeights;
	
	public StructuredPerceptron(Corpus corpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets, QuestionIdConfig config,
			String qlabel) {
		super(corpus, trainSet, testSets, config, qlabel);
	}
	
	public void run(int maxNumIterations, double learningRate) {
		// initialize weights
		weights = new double[numFeatures];
		avgWeights = new double[numFeatures];
		Arrays.fill(weights, 0.0);
		Arrays.fill(avgWeights, 0.0);
		
		double lr = learningRate;
		QGenFactorGraph model = new QGenFactorGraph(potentialFunction);
		for (int t = 0; t < maxNumIterations; t++) {
			double error = .0;
			for (QGenSequence seq : sequences) {
				if (!seq.isLabeled) {
					continue;
				}
				// Find best sequence under current weights
				model.computeScores(seq.sequenceId, weights, 0.0);
				int[] decoded = model.viterbi();
				for (int i = 0; i < seq.latticeIds.length; i++) {
					if (seq.latticeIds[i] != decoded[i]) {
						error ++;
					}
				}
				/*
				if (t > 50) {
					System.out.println(StrUtils.intArrayToString("\t", seq.latticeIds));
					System.out.println(StrUtils.intArrayToString("\t", decoded) + "\n");
				}
				*/
				for (int i = 0; i < seq.cliqueIds.length; i++) {
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, seq.cliqueIds[i], weights, lr);
					// TODO: confirm here
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, potentialFunction.getCliqueId(i, decoded),
							weights, -lr);
				}
				for (int i = 0; i < numFeatures; i++) {
					avgWeights[i] += weights[i];
				}
			}
			System.out.println(
					String.format("Iteration::%d\tParameter norm::%f\tError::%f",
							t, LatticeUtils.L2NormSquared(weights),
							error / numTrains));
		}
		for (int i = 0; i < numFeatures; i++) {
			avgWeights[i] /= (maxNumIterations * numTrains);
		}
		for (QGenSequence seq : sequences) {
			if (seq.isLabeled) {
				continue;
			}
			System.out.println(seq.sentence.getTokensString());
			System.out.println(seq.sentence.getTokenString(seq.propHead));
			// Find best sequence under current weights
			model.computeScores(seq.sequenceId, avgWeights, 0.0);
			//int[] decoded = model.viterbi();
			//System.out.println(getQuestion(seq.sentence, seq.propHead, decoded));
			System.out.println("*" + getQuestion(seq.sentence,
					seq.propHead, seq.latticeIds));
			int[][] kdecoded = model.kbestViterbi(5);
			for (int k = 0; k < 5; k++) {
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
	
	public void evaluate() {
		
	}
}
