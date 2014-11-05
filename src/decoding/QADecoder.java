package decoding;

import java.util.ArrayList;
import java.util.Arrays;

import util.LatticeUtils;
import data.DepSentence;
import data.QAPair;

public class QADecoder {

	
	// TODO: define this.
	/**
	 * input: augmented weights, QA pair
	 * output: a partial tree in adjacency form,
	 *  with 1 specifying "must" link, 0 denoting "cannot" link
	 * 	with -1 denoting "unspecified" 
	 */
	public void decode(DepSentence sentence, QAPair qa, double[][] u,
					   int[][] bestDecoded) {
		assert (bestDecoded.length == sentence.length + 1);
		ArrayList<Integer> questionTokens = flagsToList(qa.questionAlignment),
						   answerTokens = flagsToList(qa.answerAlignment);
		
		// Enumerate best QA head.
		double bestScore = Double.NEGATIVE_INFINITY;
		int[][] decoded = new int[bestDecoded.length][bestDecoded.length];
		int bestQuestionHead = -1, bestAnswerHead = -1;
		
		for (int qt : questionTokens) {
			for (int at : answerTokens) {
				// Construct tentative structure
				LatticeUtils.fill(decoded, -1);
				setParent(decoded, qt + 1, at + 1);
				setAnswerSubtree(decoded, qa.answerAlignment, answerTokens);
				// Compute score for the structure.
				double score = computeScore(decoded, u);
				if (score > bestScore) {
					bestScore = score;
					bestQuestionHead = qt;
					bestAnswerHead = at;
					LatticeUtils.copy(bestDecoded, decoded);
				}
			}
		}
		System.out.println(bestQuestionHead + ", " + bestAnswerHead + ", " +
						   bestScore);
		// Construct partial tree structure.
	}
	
	private double computeScore(int[][] decoded, double[][] u) {
		double score = 0.0;
		for (int i = 0; i < decoded.length; i++) {
			for  (int j = 0; j < decoded.length; j++) {
				if (decoded[i][j] == 1) {
					score -= u[i][j];
				}
			}
		}
		return score;
	}
	
	private ArrayList<Integer> flagsToList(int[] flags) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] != -1) {
				indices.add(flags[i]);
			}
		}
		return indices;
	}
	
	private void setParent(int[][] tree, int parent, int child) {
		tree[child][parent] = 0;
		for (int i = 0; i < tree.length; i++) {
			tree[i][child] = (parent == i ? 1 : 0);
		}
	}
	
	// Warning: need to add 1 for each index.
	private void setAnswerSubtree(int[][] tree, int[] answerFlags,
							ArrayList<Integer> answerTokens) {
		for (int id : answerTokens) {
			tree[0][id + 1] = tree[id + 1][0] = 0;
			for (int i = 0; i < answerFlags.length; i++) {
				if (answerFlags[i] == -1) {
					tree[i + 1][id + 1] = tree[id + 1][i + 1] = 0;
				}
			}		
		}
	}
	
	// Temporary solution.
	// Assign "votes" to edges that comply with the constraints.
	public void vote(DepSentence sentence, QAPair qa, double[][] scores) {
		assert (scores.length == sentence.length + 1);
		
		// Get inverse alignment.
		int[] inverseAnswerAlignment = new int[sentence.length];
		ArrayList<Integer> answerWords = new ArrayList<Integer>();
		ArrayList<Integer> questionWords = new ArrayList<Integer>();
		Arrays.fill(inverseAnswerAlignment, -1);
		
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int aword = qa.answerAlignment[i];
			if (aword != -1) {
				inverseAnswerAlignment[aword] = i;
				answerWords.add(aword);
			}
		}
		for (int i = 0; i < qa.questionAlignment.length; i++) {
			int qword = qa.questionAlignment[i];
			if (qword != -1) {
				questionWords.add(qword);
			}
		}
		
		// Select the question word that's closest to answer.
		/*
		int minAnswerWord = answerWords.get(0),
			maxAnswerWord = answerWords.get(answerWords.size() - 1);
		int questionHead = -1, // answerHead = -1,
			minQADist = sentence.length;
		for (int i = 0; i < qa.questionAlignment.length; i++) {
			int qword = qa.questionAlignment[i];
			if (qword == -1) {
				continue;
			}
			int dist = Math.min(Math.abs(qword - minAnswerWord),
								Math.abs(qword - maxAnswerWord));
			if (dist < minQADist) {
				questionHead = qword;
				minQADist = dist;
			}
		}
		*/
		// Enumerate all question-answer pairs to change the vote.
		// TODO: normalize the edge scores in some way.
		for (int qword1 : questionWords) {
			for (int qword2 : questionWords) {
				if (qword1 != qword2) {
					scores[qword1 + 1][qword2 + 1] += 1;
				}
			}
		}
		for (int aword1 : answerWords) {
			for (int aword2 : answerWords) {
				if (aword1 != aword2) {
					scores[aword1 + 1][aword2 + 1] += 1;
				}
			}
		}
		for (int qword : questionWords) { 
			for (int aword : answerWords) {
				scores[qword + 1][aword + 1] += 1;
			}
		}
	}
			
}
