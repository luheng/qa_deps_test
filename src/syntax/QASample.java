package syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import util.StringUtils;
import data.QAPair;
import data.SRLSentence;
import data.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ScoredObject;

public class QASample {
	QAPair qa;
//	int[] answerSpans;
	int propHead, answerHead;
	ArrayList<int[]> answerSpans;
	Sentence sentence;
	ArrayList<Double> kBestScores;
	ArrayList<Collection<TypedDependency>> kBestParses;
	String[] postags;
	String[] lemmas;
	
	public boolean isPositiveSample;
	
	
	private QASample(QAPair qa,
			int answerHead,
			ArrayList<int[]> answerSpans,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas,
			boolean positive) {
		this.qa = qa;
		this.sentence = qa.sentence;
		this.propHead = qa.propHead;
		this.answerHead = answerHead;
		this.answerSpans = answerSpans;
		this.kBestScores = kBestScores;
		this.kBestParses = kBestParses;
		this.postags = postags;
		this.lemmas = lemmas;
		this.isPositiveSample = positive;
	}
	
	public static QASample addPositiveSample(QAPair qa,
			int answerHead,
			ArrayList<int[]> answerSpans,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(qa,
				answerHead,
				answerSpans,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				true /* is a positive sample or not */);
	}
	
	public static QASample addNegativeSample(QAPair qa,
			int answerHead,
			ArrayList<int[]> answerSpans,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(qa,
				answerHead,
				answerSpans,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				false /* is a positive sample or not*/);
	}
	
	// TODO: Save and Load from data
	
	
	
	@Override
	public String toString() {
		return String.format("%s\n%d\t%s\n%d\t%s\n \t[%s]\t%s\n%d\t[%s]\t%s\n%s\t%s",
				(isPositiveSample ? "[POSITIVE]" : "[NEGATIVE]"),
				sentence.sentenceID, sentence.getTokensString(),
				propHead, sentence.getTokenString(propHead),
				qa.getQuestionLabel(), qa.getQuestionString(),
				answerHead, sentence.getTokenString(answerHead), qa.getAnswerString(),
				StringUtils.join("\t", postags),
				kBestParses.get(0));
	//			StringUtils.join("\t", lemmas));
	}
	
}
