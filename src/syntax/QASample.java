package syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import util.StringUtils;
import data.QAPair;
import data.SRLSentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ScoredObject;

public class QASample {
	QAPair qa;
//	int[] answerSpans;
	int propHead, answerHead;
	SRLSentence sentence;
	ArrayList<Double> kBestScores;
	ArrayList<Collection<TypedDependency>> kBestParses;
	String[] postags;
	String[] lemmas;
	
	boolean isPositiveExample;
	
	
	private QASample(QAPair qa,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas,
			boolean positive) {
		this.qa = qa;
		this.sentence = qa.sentence;
		this.propHead = qa.propHead;
		this.answerHead = answerHead;
		this.kBestScores = kBestScores;
		this.kBestParses = kBestParses;
		this.postags = postags;
		this.lemmas = lemmas;
		this.isPositiveExample = positive;
	}
	
	public static QASample addPositiveSample(QAPair qa,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(qa,
				answerHead,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				true /* is a positive sample or not */);
	}
	
	public static QASample addNegativeSample(QAPair qa,
			int answerHead,
			ArrayList<Double> kBestScores,
			ArrayList<Collection<TypedDependency>> kBestParses,
			String[] postags,
			String[] lemmas) {
		return new QASample(qa,
				answerHead,
				kBestScores,
				kBestParses,
				postags,
				lemmas,
				false /* is a positive sample or not*/);
	}
	
	
	@Override
	public String toString() {
		return String.format("%d\t%s\n%d\t%s\n%s\n[%s]\t%s\n%s",
				sentence.sentenceID, sentence.getTokensString(),
				propHead, sentence.getTokenString(propHead),
				qa.getQuestionString(),
				sentence.getTokenString(answerHead), qa.getAnswerString(),
				StringUtils.join("\t", postags));
	//			StringUtils.join("\t", lemmas));
	}
	
}
