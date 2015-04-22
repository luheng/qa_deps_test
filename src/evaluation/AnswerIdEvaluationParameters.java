package evaluation;

public class AnswerIdEvaluationParameters {
	
	public enum EvaluationType {
		topKSpan, // If top-k predicted tokens are in any of the annotated answer spans
		topKHead, // If top-k predicted tokens match with any of the answer heads
		BinarySpan,
		BinaryHead,
		topKMultiSpan,
		topKMultiHead,
		BinaryMultiSpan,
		BinaryMultiHead,
	};
	
	public EvaluationType evalType;
	
	public int kBest = 1;
	public double threshold = 1e-10; // A bit higher than 0.
	
	public AnswerIdEvaluationParameters(EvaluationType evalType) {
		this.evalType = evalType;
	}
	
	public AnswerIdEvaluationParameters(EvaluationType evalType, int kBest,
			double threshold) {
		this.evalType = evalType;
		this.kBest = kBest;
		this.threshold = threshold;
	}
	
	public boolean evalBinary() {
		return evalType == EvaluationType.BinaryHead ||
				evalType == EvaluationType.BinaryHead ||
				evalType == EvaluationType.BinaryMultiHead ||
				evalType == EvaluationType.BinaryMultiSpan;
	}

	public boolean evalMulti() {
		return evalType == EvaluationType.BinaryMultiHead ||
				evalType == EvaluationType.BinaryMultiSpan ||
				evalType == EvaluationType.topKMultiHead ||
				evalType == EvaluationType.topKMultiSpan;
	}
	
	public boolean evalHead() {
		return evalType == EvaluationType.BinaryHead ||
				evalType == EvaluationType.BinaryMultiHead || 
				evalType == EvaluationType.topKHead ||
				evalType == EvaluationType.topKMultiHead;
	}
}
