package qg;

import java.util.ArrayList;
import java.util.Arrays;

import learning.QADataset;
import learning.QASample;
import util.LatticeUtils;
import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlots;
import data.Corpus;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;

public class StructuredPerceptron {
	private Corpus baseCorpus;
	private VerbInflectionDictionary inflDict;
	
	public QGenDataset trainSet;
	public ArrayList<QGenDataset> testSets;
	public ArrayList<QGenSequence> sequences;
	public int numFeatures, numSequences, numTrains;
	
	QGenPotentialFunction potentialFunction;
	QGenFeatureExtractor featureExtractor;
	public double[] weights, avgWeights;
	
	private static final int minFeatureFreq = 5;
	
	public StructuredPerceptron(Corpus corpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets) {
		this.trainSet = trainSet;
		this.testSets = testSets;
		this.baseCorpus = corpus;
		initializeSequences();
	}
	
	public void run(int maxNumIterations, double learningRate) {
		// initialize weights
		weights = new double[numFeatures];
		avgWeights = new double[numFeatures];
		Arrays.fill(weights, 0.0);
		Arrays.fill(avgWeights, 0.0);
		
		double lr = learningRate;
		QGenFactorGraph model = new QGenFactorGraph(potentialFunction);
		for (int t = 0; t < maxNumIterations; t++) {
			for (QGenSequence seq : sequences) {
				if (!seq.isLabeled) {
					continue;
				}
				// Find best sequence under current weights
				model.computeScores(seq, weights, 0.0);
				int[] decoded = model.viterbiDecode();
				for (int i = 0; i < seq.cliqueIds.length; i++) {
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, seq.cliqueIds[i], weights, lr);
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, decoded, weights, -lr);
				}
				for (int i = 0; i < numFeatures; i++) {
					avgWeights[i] += weights[i];
				}
				if (t == 99) {
					for (int i = 0; i < decoded.length; i++) {
						System.out.print(potentialFunction.lattice[i][decoded[i]] + "\t");
					}
					System.out.println();
				}
			}
			System.out.println(
					String.format("Iteration::%d\tParameter norm::%f",
							t, LatticeUtils.L2NormSquared(weights)));
		}
		for (int i = 0; i < numFeatures; i++) {
			avgWeights[i] /= (maxNumIterations * numTrains);
		}
	}
	
	public void evaluate() {
		
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
		numTrains = sequences.size();
		for (QADataset testSet : testSets) {
			for (QASample sample : testSet.samples) {
				Sentence sentence = testSet.sentenceMap.get(sample.sentenceId);
				sequences.add(initializeSequence(sentence, sample, false));
			}
		}
		numSequences = sequences.size();
		

		System.out.println(String.format("Processing %d instances.",
				numSequences));
		System.out.println("Extract features ...");
		potentialFunction.extractFeatures(sequences, featureExtractor);
		numFeatures = featureExtractor.numFeatures();
		System.out.println(String.format("Extracted %d features.", numFeatures));
	}
	
	private QGenSequence initializeSequence(Sentence sentence, QASample sample,
			boolean isLabeled) {
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
		return new QGenSequence(sequences.size(), sentence, sample,
				latticeIds, cliqueIds, isLabeled);
	}
	
	private String getGenericTrg(Sentence sent, int propHead, String aux,
			String trg) {
		String[] twords = trg.trim().split(" ");
		String verb = sent.getTokenString(propHead).toLowerCase();
		String[] infl = inflDict.getBestInflections(verb);
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
}
