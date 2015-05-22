package decoding;

import java.util.ArrayList;
import java.util.Arrays;

import scorer.QuestionAnswerScorer;
import util.LatticeUtils;
import data.AnnotatedDepSentence;
import data.DepSentence;
import data.QAPairOld;

public class QADecoder {

	
	// TODO: define this.
	/**
	 * input: augmented weights, QA pair
	 * output: a partial tree in adjacency form,
	 *  with 1 specifying "must" link, 0 denoting "cannot" link
	 * 	with -1 denoting "unspecified" 
	 */
	public double decode(DepSentence sentence, QAPairOld qa, double[][] u,
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
		//System.out.println(sentence.getTokensString());
		/*
		System.out.println(StringUtils.join(" ", qa.questionTokens) + "\t" +
						   StringUtils.join(" ", qa.answerTokens));
		System.out.println(sentence.getTokenString(bestQuestionHead) + ", " +
						   sentence.getTokenString(bestAnswerHead) + ", " +
						   bestScore);
		*/
		return bestScore;
	}
	
	/**
	 * Simple adaptation of the QA scorer.
	 * @param sentence
	 * @param qa
	 * @param u
	 * @param bestDecoded
	 * @return
	 */
	public double decodeEntireSentence(DepSentence sentence, QAPairOld qa,
									   double[][] u, int[] bestDecoded) {
		
		QuestionAnswerScorer scorer = new QuestionAnswerScorer();
		ViterbiDecoder decoder = new ViterbiDecoder();
		double[][] scores = new double[sentence.length + 1][sentence.length + 1];
		scorer.getScores(scores, sentence, qa);
		LatticeUtils.addTo(scores, u, -1.0);
		return decoder.decode(scores, bestDecoded);
	}
	
	public double decodeEntireSentence(AnnotatedDepSentence sentence, double[][] u,
									   int[] bestDecoded) {
		QuestionAnswerScorer scorer = new QuestionAnswerScorer();
		ViterbiDecoder decoder = new ViterbiDecoder();
		int length = sentence.depSentence.length + 1;
		double[][] scores = new double[length][length],
				   tempScores = new double[length][length];
		LatticeUtils.fill(scores, 0.0);
		for (QAPairOld qa  : sentence.qaList) {
			scorer.getScores(tempScores, sentence.depSentence, qa);
			LatticeUtils.addTo(scores, tempScores);
		}
		LatticeUtils.addTo(scores, u, -1.0); 
		return decoder.decode(scores, bestDecoded);
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
	public void vote(DepSentence sentence, QAPairOld qa, double[][] scores) {
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
