package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import optimization.gradientBasedMethods.LBFGS;
import optimization.gradientBasedMethods.Optimizer;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.linesearch.WolfRuleLineSearch;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.NormalizedValueDifference;
import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;

public class QGenCRF {
	private Corpus baseCorpus; 
	private VerbInflectionDictionary inflDict;
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	String[][] latticeTemplate;
	private static int minFeatureFreq = 5;
	
	QGenFeatureExtractor featureExtractor;
	int[][][] latticeSizes; // instance-id, slot-id, 3
	
	ArrayList<QGenSequence> sequences;
	int numSequences;
	QGenPotentialFunction potentialFunction;
	
	int numFeatures;
	double[] parameters;
	double[] empiricalCounts;
	
	public QGenCRF(Corpus baseCorpus, QuestionIdDataset trainSet,
			HashMap<String, QuestionIdDataset> testSets) {
		this.baseCorpus = baseCorpus;
		this.trainSet = trainSet;
		this.testSets = testSets;
		initializeSequences();
		potentialFunction.extractFeatures(trainSet, featureExtractor);
	}
	
	public void run() {
		LineSearchMethod lineSearch;
		CompositeStopingCriteria stopping;
		Optimizer optimizer;
		OptimizerStats stats;
		double prevStepSize = 0.1;
		QGenCRFObjective objective;
		int numIters = 100;
		double stopThreshold = 1e-4;
		double gaussianPrior = 1.0;
		
		// ******* initialize model
		numFeatures = featureExtractor.featureDict.size();
		parameters = new double[numFeatures];
		empiricalCounts = new double[numFeatures];
		for (QGenSequence sequence : sequences) {
			if (!sequence.isLabeled) {
				continue;
			}
			for (int i = 0; i < QGenSlots.numSlots; i++) {
				potentialFunction.addToEmpirical(sequence.sequenceId, i,
						sequence.latticeIds, empiricalCounts, 1.0);
			}
		}
		lineSearch = new WolfRuleLineSearch(
				new InterpolationPickFirstStep(prevStepSize), 1e-4, 0.9, 10);
		lineSearch.setDebugLevel(0);
		stopping = new CompositeStopingCriteria();
		stopping.add(new NormalizedValueDifference(stopThreshold));
		optimizer = new LBFGS(lineSearch, 10);
		optimizer.setMaxIterations(numIters);
		stats = new OptimizerStats();
		objective = new QGenCRFObjective(
				new QGenFactorGraph(potentialFunction),
				sequences, parameters, empiricalCounts, gaussianPrior); 
		boolean succeed = optimizer.optimize(objective, stats, stopping);
		prevStepSize = optimizer.getCurrentStep();
		System.out.println("success:\t" + succeed + "\twith latest stepsize:\t"
				+ prevStepSize);
	
		double obj = objective.objective;
			
		System.out.println("Negative Labeled Likelihood::\t" +
				objective.labelLikelihood);
		System.out.println("*** Combined objective::\t" + obj);
	}
	
	private void initializeSequences() {
		featureExtractor = new QGenFeatureExtractor(baseCorpus, minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		potentialFunction = new QGenPotentialFunction();
		
		sequences = new ArrayList<QGenSequence>();
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					sequences.add(initializeSequence(sent, propHead, qa, true));
				}
			}
		}
		for (QuestionIdDataset testSet : testSets.values()) {
			for (AnnotatedSentence sent : testSet.sentences) {
				for (int propHead : sent.qaLists.keySet()) {
					for (QAPair qa : sent.qaLists.get(propHead)) {
						sequences.add(initializeSequence(sent, propHead, qa,
								false));
					}
				}
			}
		}
		numSequences = sequences.size();
		System.out.println(String.format("Processing %d instances.",
				numSequences));
	}
	
	private QGenSequence initializeSequence(AnnotatedSentence sent,
			int propHead, QAPair qa, boolean isLabeled) {
		String[][] lattice = potentialFunction.lattice;
		int[] latticeIds = new int[lattice.length],
			  cliqueIds = new int[lattice.length];
		for (int i = 0; i < lattice.length; i++) {
			if (i == QGenSlots.TRGSlotId) {
				// TODO
				continue;
			}
			for (int j = 0; j < lattice[i].length; j++) {
				if (lattice[i][j].equalsIgnoreCase(qa.questionWords[i])) {
					latticeIds[i] = j;
					break;
				}
			}
		}
		for (int i = 0; i < lattice.length; i++) {
			cliqueIds[i] = potentialFunction.getCliqueId(i, latticeIds);
		}
		return new QGenSequence(sequences.size(), sent.sentence, propHead,
				latticeIds, cliqueIds, isLabeled);
	}
	

}
