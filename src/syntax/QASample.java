package syntax;

import java.util.List;

import data.QAPair;
import data.SRLSentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.ScoredObject;

public class QASample {
	QAPair qa;
//	int[] answerSpans;
	int propHead, answerHead;
	SRLSentence sentence;
	List<ScoredObject<Tree>> kBestParses;
	
	boolean isPositiveExample;
	
	
	public QASample(QAPair qa,
					int answerHead,
					List<ScoredObject<Tree>> kBestParses) {
		this.qa = qa;
		this.sentence = qa.sentence;
		this.propHead = qa.propHead;
		this.answerHead = answerHead;
		this.kBestParses = kBestParses;
	}
	
	
}
