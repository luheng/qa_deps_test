package scorer;

import java.util.ArrayList;

import util.LatticeUtils;
import data.DepSentence;
import data.QAPairOld;

/**
 * Heuristic scorer based on question-answer annotation.
 * @author luheng
 *
 */
public class QuestionAnswerScorer {
	
	private boolean addIntraQuestionEdges = true;
	private boolean addIntraAnswerEdges = true;
	private boolean addReverseQAEdges = false;
	
	public void getScores(double[][] scores, DepSentence sentence, QAPairOld qa) {
		ArrayList<Integer> questionWords = flagsToList(qa.questionAlignment),
						   answerWords = flagsToList(qa.answerAlignment);
		
		LatticeUtils.fill(scores, 0.0);
		
		if (this.addIntraQuestionEdges) {
			for (int q1 : questionWords) {
				for (int q2 : questionWords) {
					if (q1 != q2) {
						scores[q1 + 1][q2 + 1] += 1.0;
					}
				}
			}
		}
		if (this.addIntraAnswerEdges) {
			for (int a1 : answerWords) {
				for (int a2 : answerWords) {
					if (a1 != a2) {
						scores[a1 + 1][a2 + 1] += 1.0;
					}
				}
			}
		}
		for (int q1 : questionWords) {
			for (int a1 : answerWords) {
				if (q1 != a1) {
					scores[q1 + 1][a1 + 1] += 1.0;
					if (this.addReverseQAEdges) {
						scores[a1 + 1] [q1 + 1] += 1.0;
					}
				}
			}
		} 
	}
	
	public void setAddIntraQuestionEdges(boolean flag) {
		this.addIntraQuestionEdges = flag;
	}
	
	public void setAddIntraAnswerEdges(boolean flag) {
		this.addIntraAnswerEdges = flag;
	}
	
	public void setAddReverseQAEdges(boolean flag) {
		this.addReverseQAEdges = flag;
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
}
