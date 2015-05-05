package learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import learning.BeamSearch.Beam;
import annotation.QASlotAuxiliaryVerbs;
import optimization.gradientBasedMethods.LBFGS;
import optimization.gradientBasedMethods.Optimizer;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.linesearch.WolfRuleLineSearch;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.NormalizedValueDifference;
import util.LatticeUtils;
import data.Corpus;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;

public class QGenCRF {
	private Corpus baseCorpus; 
	private VerbInflectionDictionary inflDict;
	private QGenDataset trainSet;
	private HashMap<String, QGenDataset> testSets;
	
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
	
	public QGenCRF(Corpus baseCorpus, QGenDataset trainSet,
			HashMap<String, QGenDataset> testSets) {
		this.baseCorpus = baseCorpus;
		this.trainSet = trainSet;
		this.testSets = testSets;
		initializeSequences();
		potentialFunction.extractFeatures(sequences, featureExtractor);
	}
	
	public void run() {
		LineSearchMethod lineSearch;
		CompositeStopingCriteria stopping;
		Optimizer optimizer;
		OptimizerStats stats;
		double prevStepSize = 1e-4;
		QGenCRFObjective objective;
		int numIters = 30;
		double stopThreshold = 1e-3;
		double gaussianPrior = 10;
		
		System.out.println("Start CRF training");
		
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
		System.out.println("Empirical count l2norm:\t" +
				LatticeUtils.L2NormSquared(empiricalCounts));
		
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
		
		predict();
	}
	
	public void predict() {
		final int beamSize = 1000;
		final int topK = 10;
		for (int seq = 0; seq < 1; seq++) {
			QGenSequence sequence = sequences.get(seq); 
			QGenFactorGraph graph = new QGenFactorGraph(potentialFunction);
			graph.computeScores(sequence, parameters, 0);
			BeamSearch bs = new BeamSearch(sequence, graph, potentialFunction,
					beamSize);
			PriorityQueue<Beam> kBest = bs.getTopK(topK);
			for (Beam beam : kBest) {
				System.out.print(beam.loglik + "\t");
				for (int i = 0; i < QGenSlots.numSlots; i++) {
					//System.out.println(i + ", " + beam.ids[i]);
					int id = beam.ids[i + 2];
					System.out.print(potentialFunction.lattice[i][id] + "\t");
				}
				System.out.println();
			}
		}
	}
	
	private void initializeSequences() {
		featureExtractor = new QGenFeatureExtractor(baseCorpus, minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		potentialFunction = new QGenPotentialFunction();
		
		sequences = new ArrayList<QGenSequence>();
		for (QASample sample : trainSet.samples) {
			Sentence sentence = trainSet.sentenceMap.get(sample.sentenceId);
			sequences.add(initializeSequence(sentence, sample, true));
		}
		for (QGenDataset testSet : testSets.values()) {
			for (QASample sample : testSet.samples) {
				Sentence sentence = testSet.sentenceMap.get(sample.sentenceId);
				sequences.add(initializeSequence(sentence, sample, false));
			}
		}
		numSequences = sequences.size();
		System.out.println(String.format("Processing %d instances.",
				numSequences));
	}
	
	private QGenSequence initializeSequence(Sentence sentence, QASample sample,
			boolean isLabeled) {
		String[][] lattice = potentialFunction.lattice;
		String[] question = sample.question;
		int[] latticeIds = new int[lattice.length],
			  cliqueIds = new int[lattice.length];
		for (int i = 0; i < lattice.length; i++) {
			String token = question[i];
			if (i == QGenSlots.TRGSlotId) {
				token = getGenericTrg(sentence, sample.propHead,
						question[QGenSlots.AUXSlotId], question[i]);
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
		return new QGenSequence(sequences.size(), sentence, sample,
				latticeIds, cliqueIds, isLabeled);
	}
	
	private String getGenericTrg(Sentence sent, int propHead, String aux,
			String trg) {
		String[] twords = trg.trim().split(" ");
		String[] infl = getInflections(sent, propHead);
		if (twords.length > 1 && trg.endsWith("ing")) {
			return twords.length == 2 ?
						twords[0] + " doing" :
						twords[0] + " " + twords[1] + " doing";
		}
		if (twords.length > 1) {
			return twords.length == 2 ?
						twords[0] + " done" :
						twords[0] + " " + twords[1] + " done";
		}
		if (twords[0].equals(infl[1])) {
			return "does";
		}
		if (twords[0].equals(infl[2])) {
			return "doing";
		}
		if (twords[0].equals(infl[3]) && aux.isEmpty()) {
			return "did";
		}
		if (twords[0].equals(infl[4]) && 
				(QASlotAuxiliaryVerbs.beValuesSet.contains(aux) ||
				 QASlotAuxiliaryVerbs.haveValuesSet.contains(aux))) {
			return "done";
		}
		return "do";
	}
	
	private String[] getInflections(Sentence sent, int propHeadId) {
		String verb = sent.getTokenString(propHeadId).toLowerCase();
		String verbPrefix = "";
		if (verb.contains("-")) {
			int idx = verb.indexOf('-');
			verbPrefix = verb.substring(0, idx + 1);
			verb = verb.substring(idx + 1);
			// System.out.println(verbPrefix + ", " + verb);
		}
		ArrayList<Integer> inflIds = inflDict.inflMap.get(verb);
		if (inflIds == null) {
			return null;
		}
		int bestId = -1, bestCount = -1;
		for (int i = 0; i < inflIds.size(); i++) {
			int count = inflDict.inflCount[inflIds.get(i)];
			if (count > bestCount) {
				bestId = inflIds.get(i);
				bestCount = count;
			}
		}
		String[] inflections = new String[5];
		for (int i = 0; i < 5; i++) {
			inflections[i] = verbPrefix + inflDict.inflections.get(bestId)[i];
		}
		return inflections;
	}

}
