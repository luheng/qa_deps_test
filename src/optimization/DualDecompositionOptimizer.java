package optimization;

import java.util.ArrayList;

import scorer.DistanceScorer;
import scorer.UniversalGrammarScorer;
import util.LatticeUtils;
import data.Accuracy;
import data.AnnotatedSentence;
import data.DepSentence;
import data.Evaluation;
import decoding.Decoder;
import decoding.QADecoder;

public class DualDecompositionOptimizer {

	public void run(ArrayList<AnnotatedSentence> instances,
					Decoder decoder,
					QADecoder qaDecoder,
					int maxNumIterations,
					double initialStepSize) {
		
		Accuracy accuracy = new Accuracy(0, 0);
		System.out.println("========== Optimization Starts =========");
		
		// Use scorers.
		DistanceScorer distScorer = new DistanceScorer();
		UniversalGrammarScorer ugScorer =
				new UniversalGrammarScorer(instances.get(0).depSentence.corpus);
		
		for (AnnotatedSentence sentence : instances) {
			int length = sentence.depSentence.length + 1;
			int numQAs = sentence.qaList.size();
			double[][][] u = new double[numQAs][length][length];
			double[][] scores = new double[length][length],
					   tempScores = new double[length][length];
			int[] y = new int[length - 1];
			int[][][] z = new int[numQAs][length][length];
			// Number of components for each edge.
			int[][] cnt = new int[length][length];
			
			LatticeUtils.fill(u, 0.0);
			
			for (int iter = 0; iter < maxNumIterations; iter++) {
				double eta = initialStepSize / (1.0 + iter);
				
				// y* = argmax f(y) + u.y
				// simply take an average of the constraint scores.
				DepSentence depSentence = sentence.depSentence;
				distScorer.getScores(scores, depSentence);
				ugScorer.getScores(tempScores, depSentence);
				LatticeUtils.addTo(scores, tempScores, 1.0);
				
				if (iter > 0) {
					for (int i = 0; i < numQAs; i++) {
						for (int j = 0; j < length; j++) {
							for (int k = 0; k < length; k++) {
								if (cnt[j][k] > 0) {
									scores[j][k] += u[i][j][k] / cnt[j][k];
								}
							}
						}
					}
				}
				// Print scores
				// TODO: instead of printing out the score matrix, print out
				// token pairs that receive highest scores.
				/*
				for (int i = 0; i < scores.length; i++) {
					System.out.println(
							StringUtils.doubleArrayToString("\t", scores[i]));
				}
				*/
				
				double obj1 = decoder.decode(scores, y),
					   obj2 = 0.0;
				
				// z* = argmax h(z) - u.z
				for (int i = 0; i < numQAs; i++) {
					obj2 += qaDecoder.decode(sentence.depSentence,
								sentence.qaList.get(i), u[i], z[i]);
				}
				
				LatticeUtils.fill(cnt, 0);
				
				// Update u <- u + eta * (z - y)
				for (int i = 0; i < numQAs; i++) {
					for (int j = 0; j < length; j++) {
						for (int k = 1; k < length; k++) {
							if (z[i][j][k] == 1 && y[k - 1] != j - 1) {
								u[i][j][k] += eta;
							} else if (z[i][j][k] == 0 && y[k - 1] == j - 1) {
								u[i][j][k] -= eta;
							}
							if (z[i][j][k] != -1) {
								cnt[j][k] ++;
							}
						}
					}
				}
				// TODO: Print objective
				// Print accuracy
				Accuracy acc = Evaluation.getAccuracy(sentence.depSentence, y);
				System.out.println(String.format(
						"ITER:: %d\t OBJ_Y:: %.3f\t OBJ_Z:: %.3f\t U_NORM:: %.3f\t ACC:: %.3f",
						iter, obj1, obj2,
						getL2Norm(u),
						acc.accuracy()));
			}
			// Evaluate, based on the last y
			Accuracy acc = Evaluation.getAccuracy(sentence.depSentence, y);
			accuracy.add(acc);
			System.out.println();
		}
		System.out.println("accuracy:\t" + accuracy.accuracy());
	}
	
	private double getL2Norm(double[][][] u) {
		double norm = 0.0;
		for (int i = 0; i < u.length; i++) {
			for (int j = 0; j < u[i].length; j++) {
				for (int k = 0; k < u[i][j].length; k++) {
					norm += u[i][j][k] * u[i][j][k];
				}
			}
		}
		return norm;
	}
}
