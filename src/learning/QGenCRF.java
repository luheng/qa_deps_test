package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;
import io.XSSFOutputHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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
import data.Sentence;
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
	
	int[][][][] featureIds;     // seq-id, slot-id, clique-id, feat-id
	double[][][][] featureVals;
	
	int numFeatures;
	double[] parameters;
	double[] empiricalCounts;
	
	public QGenCRF(Corpus baseCorpus, QuestionIdDataset trainSet,
			HashMap<String, QuestionIdDataset> testSets) {
		this.baseCorpus = baseCorpus;
		this.trainSet = trainSet;
		this.testSets = testSets;
		initializeSequences();
		extractFeatures();
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
		objective = new QGenCRFObjective(sequences, parameters,
				empiricalCounts, gaussianPrior); 
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
	
	private void extractFeatures() {
		int seqLength = QGenSlots.numSlots;
		int[] csizes = potentialFunction.cliqueSizes;
		int[][] iterator = potentialFunction.iterator;
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				for (int i = 0; i < seqLength; i++) {
					for (int s = 0; s < iterator[i][0]; s++) {
						for (int sp = 0; sp < iterator[i][1]; sp++) {
							for (int spp = 0; spp < iterator[i][2]; spp++) {
								featureExtractor.extractFeatures(
										sent.sentence, propHead,
										potentialFunction.lattice,
										i, s, sp, spp, true /* accept new */);
							}
						}
					}
				}
			}
			System.out.println(sent.sentence.sentenceID + "\t" +
							   featureExtractor.featureDict.size());
		}
		featureExtractor.freeze();
		numFeatures = featureExtractor.featureDict.size();
		System.out.println(String.format("Extracted %d features.", numFeatures));
		
		featureIds = new int[numSequences][seqLength][][];
		featureVals = new double[numSequences][seqLength][][];
		for (int seq = 0; seq < numSequences; seq++) {
			QGenSequence sequence = sequences.get(seq);
			for (int i = 0; i < seqLength; i++) {
				featureIds[seq][i] = new int[csizes[i]][];
				featureVals[seq][i] = new double[csizes[i]][];
				for (int s = 0; s < iterator[i][0]; s++) {
					for (int sp = 0; sp < iterator[i][1]; sp++) {
						for (int spp = 0; spp < iterator[i][2]; spp++) {
							int cliqueId =
								potentialFunction.getCliqueId(i, s, sp, spp);
							TIntDoubleHashMap fv =
								featureExtractor.extractFeatures(
									sequence.sentence, sequence.propHead,
									potentialFunction.lattice,
									i, s, sp, spp, false /* accept new */);
							System.out.println(fv.size());
							populateFeatureVector(seq, i, cliqueId, fv);
						}
					}
				}
			}
			System.out.println(sequence.sequenceId + "\t" + sequence.sentence.sentenceID + "\t" + sequence.propHead);
		}
		potentialFunction.setFeatures(featureIds, featureVals);
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
	

	private void populateFeatureVector(int seqId, int slotId, int cliqueId,
			TIntDoubleHashMap fv) {
		int[] fids = Arrays.copyOf(fv.keys(), fv.size());
		Arrays.sort(fids);
		featureIds[seqId][slotId][cliqueId] = new int[fids.length];
		featureVals[seqId][slotId][cliqueId] = new double[fids.length];
		for (int i = 0; i < fids.length; i++) {
			featureIds[seqId][slotId][cliqueId][i] = fids[i];
			featureVals[seqId][slotId][cliqueId][i] = fv.get(fids[i]);
		}
	}
}
