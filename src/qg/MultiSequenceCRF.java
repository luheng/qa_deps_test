package qg;

import java.util.ArrayList;
import java.util.HashMap;

import learning.QADataset;
import learning.QASample;
import optimization.gradientBasedMethods.LBFGS;
import optimization.gradientBasedMethods.Optimizer;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.linesearch.WolfRuleLineSearch;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.NormalizedValueDifference;
import annotation.QASlots;
import config.QuestionIdConfig;
import data.Corpus;
import data.Sentence;
import experiments.ExperimentUtils;

public class MultiSequenceCRF extends QGLearner {
	double[] parameters;
	public ArrayList<MultiSequence> sequences;
	
	// Maps sentences to sequence Ids
	private HashMap<Integer, Integer> sent2seq;
	
	public MultiSequenceCRF(Corpus corpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets, QuestionIdConfig config) {
		super(corpus, trainSet, testSets, config);
	}

	public void run(int maxNumIters) {
		LineSearchMethod lineSearch;
		CompositeStopingCriteria stopping;
		Optimizer optimizer;
		OptimizerStats stats;
		double prevStepSize = 1e-4;
		MultiSequenceObjective objective;
		double stopThreshold = 1e-3;
		double gaussianPrior = 10;
		
		System.out.println("Start CRF training");
		
		// ******* initialize model
		numFeatures = featureExtractor.featureDict.size();
		parameters = new double[numFeatures];
		
		lineSearch = new WolfRuleLineSearch(
				new InterpolationPickFirstStep(prevStepSize), 1e-4, 0.9, 10);
		lineSearch.setDebugLevel(0);
		stopping = new CompositeStopingCriteria();
		stopping.add(new NormalizedValueDifference(stopThreshold));
		optimizer = new LBFGS(lineSearch, 10);
		optimizer.setMaxIterations(maxNumIters);
		stats = new OptimizerStats();
		objective = new MultiSequenceObjective(
				new QGenFactorGraph(potentialFunction),
				sequences, parameters, gaussianPrior); 
		boolean succeed = optimizer.optimize(objective, stats, stopping);
		prevStepSize = optimizer.getCurrentStep();
		System.out.println("success:\t" + succeed + "\twith latest stepsize:\t"
				+ prevStepSize);
	
		double obj = objective.objective;
		System.out.println("Negative Labeled Likelihood::\t" +
				objective.labelLikelihood);
		System.out.println("*** Combined objective::\t" + obj);
		
		predict();
	}
	
	public void predict() {
		final int topK = 10;
		for (MultiSequence seq : sequences) {
			if (seq.isLabeled) {
				continue;
			}
			QGenFactorGraph graph = new QGenFactorGraph(potentialFunction);
			graph.computeScores(seq.sequenceId, parameters, 0);
			for (int[] lattice : seq.latticeIds) {
				System.out.println("*" + getQuestion(seq.sentence,
						seq.propHead, lattice));
			}
			int[][] kdecoded = graph.kbestViterbi(topK);
			for (int k = 0; k < topK; k++) {
				System.out.println(getQuestion(seq.sentence, seq.propHead,
						kdecoded[k]));
			}
			System.out.println();
		}
	}
	
	protected void initializeSequences(String qlabel /* qlabel is ignored */) {
		featureExtractor = new QGenFeatureExtractor(baseCorpus,
				config.minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		potentialFunction = new QGenPotentialFunction();
		
		sent2seq = new HashMap<Integer, Integer>();
		sequences = new ArrayList<MultiSequence>();
		for (QASample sample : trainSet.samples) {	
			Sentence sentence = trainSet.sentenceMap.get(sample.sentenceId);
			int sentId = sentence.sentenceID;
			if (!sent2seq.containsKey(sentence.sentenceID)) {
				sent2seq.put(sentId, sequences.size());
				sequences.add(new MultiSequence(sequences.size(), sentence,
						sample.propHead, true /* is labeled */));
			}
			addSequence(sequences.get(sent2seq.get(sentId)), sentence, sample);
		}
		numTrains = sequences.size();
		
		for (QADataset testSet : testSets) {
			for (QASample sample : testSet.samples) {
				Sentence sentence = trainSet.sentenceMap.get(sample.sentenceId);
				int sentId = sentence.sentenceID;
				if (!sent2seq.containsKey(sentence.sentenceID)) {
					sent2seq.put(sentId, sequences.size());
					sequences.add(new MultiSequence(sequences.size(), sentence,
							sample.propHead, false /* is labeled */));
				}
				addSequence(sequences.get(sent2seq.get(sentId)), sentence,
						sample);
			}
		}
		numSequences = sequences.size();
		System.out.println(String.format("Processing %d instances.",
				numSequences));
		System.out.println("Extract features ...");
		potentialFunction.extractMultiSequenceFeatures(sequences,
				featureExtractor);
		numFeatures = featureExtractor.numFeatures();
		System.out.println(String.format("Extracted %d features.", numFeatures));
	}
	
	private void addSequence(MultiSequence sequence, Sentence sentence,
			QASample sample) {
		String[][] lattice = potentialFunction.lattice;
		String[] question = sample.question;
		int[] latticeIds = new int[lattice.length],
			  cliqueIds = new int[lattice.length];
		for (int i = 0; i < lattice.length; i++) {
			String token = question[i];
			if (i == QASlots.TRGSlotId) {
				token = getGenericTrg(sentence, sample.propHead,
							question[QASlots.AUXSlotId], question[i]);
			}
			for (int j = 0; j < lattice[i].length; j++) {
				if (lattice[i][j].equalsIgnoreCase(token)) {
					latticeIds[i] = j;
					break;
				}
			}
		}
		for (int i = 0; i < lattice.length; i++) {
			cliqueIds[i] = potentialFunction.getCliqueId(i, latticeIds);
		}
		sequence.addSequence(sample, latticeIds, cliqueIds);
	}
}
