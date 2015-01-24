package experiments;

import java.util.ArrayList;

import optimization.DualDecompositionOptimizer;
import data.AnnotatedDepSentence;
import data.DepCorpus;
import decoding.Decoder;
import decoding.QADecoder;
import decoding.ViterbiDecoder;

public class DualDecompositionExperiment {
	
	private static DepCorpus trainCorpus;
	private static ArrayList<AnnotatedDepSentence> annotatedSentences;
	
	private static void runDualDecomposition(int numIterations,
											 double initialStepSize) {
		Decoder viterbiDecoder = new ViterbiDecoder();
		QADecoder qaDecoder = new QADecoder();
		DualDecompositionOptimizer optimizer = new DualDecompositionOptimizer();
		optimizer.run(annotatedSentences,
					  viterbiDecoder,
					  qaDecoder,
					  numIterations,
					  initialStepSize);
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		annotatedSentences =
				ExperimentUtils.loadAnnotatedSentences(trainCorpus);
		ExperimentUtils.doGreedyAlignment(annotatedSentences);
		
		//int numIterations = 20;
		//double[] stepSizes = { 0.1, 0.
		runDualDecomposition(5000, 0.1);
	}
		
}
