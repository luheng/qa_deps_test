package syntax;

import java.util.ArrayList;
import java.util.Collection;

import data.Sentence;
import edu.stanford.nlp.trees.TypedDependency;

public class QASample implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	int sourceSentenceId;
	String[] tokens;
	String[] question;
	int propHead, answerHead;
	ArrayList<Double> kBestScores;
	ArrayList<Collection<TypedDependency>> kBestParses;
	String[] postags;
	String[] lemmas;
	
	public boolean isPositiveSample;
	
	private QASample(
			Sentence sentence,
			int propHead,
			String[] question,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas,
			boolean positive) {
		this.sourceSentenceId = sentence.sentenceID;
		this.tokens = new String[sentence.length];
		for (int i = 0; i < sentence.length; i++) {
			this.tokens[i] = sentence.getTokenString(i);
		}
		this.propHead = propHead;
		this.question = question;
		this.answerHead = answerHead;
		this.kBestScores = kBestScores;
		this.kBestParses = kBestParses;
		this.postags = postags;
		this.lemmas = lemmas;
		this.isPositiveSample = positive;
	}
	
	public static QASample addPositiveSample(Sentence sentence,
			int propHead,
			String[] question,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(
				sentence,
				propHead,
				question,
				answerHead,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				true /* is a positive sample or not */);
	}
	
	public static QASample addNegativeSample(Sentence sentence,
			int propHead,
			String[] question,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(
				sentence,
				propHead,
				question,
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
