package decoding;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;

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
	public void decode(DepSentence sentence, QAPair qa, int[][] tree) {
		assert (tree.length == sentence.length + 1);
		int length = tree.length;
		for (int i = 0; i < length; i++) {
			// Initialize all edges as "unspecified".
			Arrays.fill(tree[i], -1);
		}
		// Get inverse alignment.
		int[] inverseAnswerAlignment = new int[sentence.length];
		ArrayList<Integer> answerWords = new ArrayList<Integer>();
		Arrays.fill(inverseAnswerAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int aword = qa.answerAlignment[i];
			if (aword != -1) {
				inverseAnswerAlignment[aword] = i;
				answerWords.add(aword);
			}
		}
		
		// Simplest case: enumerate question and answer head.
		// TODO: Heuristic: select the question word that's closest to answer word,
		// select
		int questionHead = -1, answerHead = -1;
		for (int i = 0; i < qa.questionAlignment.length; i++) {
			int qword = qa.questionAlignment[i];
			if (qword != -1) {
				questionHead = qword;
				break;
			}
		}
		answerHead = answerWords.get(answerWords.size() - 1);
		
		setParent(tree, questionHead + 1, answerHead + 1);
		for (int aword : answerWords) {
			if (aword != answerHead) {
				continue;
			}
			if (answerWords.size() == 2) {
				setParent(tree, answerHead + 1, aword + 1);
				break;
			} 
			for (int i = 0; i < sentence.length; i++) {
				if (inverseAnswerAlignment[i] == -1) {
					tree[i + 1][aword + 1] = 0;
				}
			}
		}
	}
	
	// Assign "votes" to edges that comply with the constraints.
	public void vote(DepSentence sentence, QAPair qa, int[][] tree) {
		assert (tree.length == sentence.length + 1);
		
		// Get inverse alignment.
		int[] inverseAnswerAlignment = new int[sentence.length];
		ArrayList<Integer> answerWords = new ArrayList<Integer>();
		Arrays.fill(inverseAnswerAlignment, -1);
		for (int i = 0; i < qa.answerAlignment.length; i++) {
			int aword = qa.answerAlignment[i];
			if (aword != -1) {
				inverseAnswerAlignment[aword] = i;
				answerWords.add(aword);
			}
		}
		int minAnswerWord = answerWords.get(0),
			maxAnswerWord = answerWords.get(answerWords.size() - 1);
		
		// Select the question word that's closest to answer.
		int questionHead = -1, // answerHead = -1,
			minQADist = sentence.length;
		for (int i = 0; i < qa.questionAlignment.length; i++) {
			int qword = qa.questionAlignment[i];
			int dist = Math.min(Math.abs(qword - minAnswerWord),
								Math.abs(qword - maxAnswerWord));
			if (dist < minQADist) {
				questionHead = qword;
				minQADist = dist;
			}
		}
		// Enumerate answer head to change the vote.
		// TODO: normalize the edge scores in some way.
		for (int answerHead : answerWords) {
			tree[questionHead][answerHead] += 1;
			for (int aword : answerWords) {
				if (aword != answerHead) {
					tree[answerHead][aword] += 1;
				}
			}
		}
	}
	
	private void setParent(int[][] tree, int parent, int child) {
		tree[child][parent] = 0;
		for (int i = 0; i < tree.length; i++) {
			tree[i][child] = (parent == i ? 1 : 0);
		}
	}
	
	public void decode(DepSentence sentence, QAPair qa, AdjacencyGraph scores,
					   int[][] tree) {
		
	}
}
