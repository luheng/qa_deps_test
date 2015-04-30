package learning;

import java.util.ArrayList;
import java.util.Collection;

import data.Sentence;
import edu.stanford.nlp.trees.TypedDependency;

public class QASample implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	public int sentenceId, questionId;
	public String[] tokens;
	public String[] question;
	public int propHead, answerWordPosition, questionLabelId;
	public String questionLabel;
	public ArrayList<Double> kBestScores;
	public ArrayList<Collection<TypedDependency>> kBestParses;
	public String[] postags;
	public String[] lemmas;
	
	public boolean isPositiveSample;
	
	private QASample(
			Sentence sentence,
			int propHead,
			int questionId,
			String[] question,
			String questionLabel,
			int questionLabelId,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas,
			boolean positive) {
		this.sentenceId = sentence.sentenceID;
		this.tokens = new String[sentence.length];
		for (int i = 0; i < sentence.length; i++) {
			this.tokens[i] = sentence.getTokenString(i);
		}
		if (propHead < 0) {
			System.out.println(sentence.sentenceID);
		}
		this.propHead = propHead;
		this.questionId = questionId;
		this.question = question;
		this.questionLabel = questionLabel;
		this.questionLabelId = questionLabelId;
		this.answerWordPosition = answerHead;
		this.kBestScores = kBestScores;
		this.kBestParses = kBestParses;
		this.postags = postags;
		this.lemmas = lemmas;
		this.isPositiveSample = positive;
	}
	
	public static QASample addPositiveAnswerIdSample(Sentence sentence,
			int propHead,
			int questionId,
			String[] question,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(
				sentence,
				propHead,
				questionId,
				question,
				"", /* question label */
				-1, /* question label id */
				answerHead,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				true /* is a positive sample or not */);
	}
	
	public static QASample addNegativeAnswerIdSample(Sentence sentence,
			int propHead,
			int questionId,
			String[] question,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(
				sentence,
				propHead,
				questionId,
				question,
				"", /* question label */
				-1, /* question label id */
				answerHead,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				false /* is a positive sample or not */);
	}
	
	// TODO: Save and Load from data
	
	@Override
	public String toString() {
		// TODO:
		return "";
	}
	
}
