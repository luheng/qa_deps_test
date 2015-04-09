package syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	
	boolean isPositiveExample;
	
	
	public QASample(QAPair qa,
					int answerHead,
					ArrayList<Double> kBestScores,
					ArrayList<Collection<TypedDependency>> kBestParses,
					String[] postags) {
		this.qa = qa;
		this.sentence = qa.sentence;
		this.propHead = qa.propHead;
		this.answerHead = answerHead;
		this.kBestScores = kBestScores;
		this.kBestParses = kBestParses;
		this.postags = postags;
	}
	
	
}
