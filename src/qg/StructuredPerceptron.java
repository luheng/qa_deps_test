package qg;

import java.util.ArrayList;
import java.util.Arrays;

import learning.QADataset;
import learning.QASample;
import util.LatticeUtils;
import util.StrUtils;
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
			double error = .0;
			for (QGenSequence seq : sequences) {
				if (!seq.isLabeled) {
					continue;
				}
				// Find best sequence under current weights
				model.computeScores(seq.sequenceId, weights, 0.0);
				int[] decoded = model.viterbi();
				for (int i = 0; i < seq.latticeIds.length; i++) {
					if (seq.latticeIds[i] != decoded[i]) {
						error ++;
					}
				}
				/*
				if (t > 50) {
					System.out.println(StrUtils.intArrayToString("\t", seq.latticeIds));
					System.out.println(StrUtils.intArrayToString("\t", decoded) + "\n");
				}
				*/
				for (int i = 0; i < seq.cliqueIds.length; i++) {
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, seq.cliqueIds[i], weights, lr);
					potentialFunction.addToEmpirical(seq.sequenceId,
							i, decoded, weights, -lr);
				}
				for (int i = 0; i < numFeatures; i++) {
					avgWeights[i] += weights[i];
				}
			}
			System.out.println(
					String.format("Iteration::%d\tParameter norm::%f\tError::%f",
							t, LatticeUtils.L2NormSquared(weights),
							error / numTrains));
		}
		for (int i = 0; i < numFeatures; i++) {
			avgWeights[i] /= (maxNumIterations * numTrains);
		}
		for (QGenSequence seq : sequences) {
			if (seq.isLabeled) {
				continue;
			}
			System.out.println(seq.sentence.getTokensString());
			System.out.println(seq.sentence.getTokenString(seq.propHead));
			// Find best sequence under current weights
			model.computeScores(seq.sequenceId, avgWeights, 0.0);
			int[] decoded = model.viterbi();
			System.out.println(getQuestion(seq.sentence, seq.propHead, decoded));
			int[][] kdecoded = model.kbestViterbi(5);
			for (int k = 0; k < 5; k++) {
				System.out.println(getQuestion(seq.sentence, seq.propHead, kdecoded[k]));
				//for (int i = 0; i < decoded.length; i++) {
				//	System.out.print(potentialFunction.lattice[i][decoded[k][i]] + "\t");
				//}
				//System.out.println();
			}
			System.out.println();
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
			/*if (!sample.questionLabel.startsWith("W0")) {
				continue;
			}*/
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
	
	public String getQuestion(Sentence sent, int propHead, int[] states) {
		String verb = sent.getTokenString(propHead).toLowerCase();
		String[] infl = inflDict.getBestInflections(verb);
		String qstr = "";
		for (int i = 0; i < states.length; i++) {
			String slot = potentialFunction.lattice[i][states[i]];
			if (slot.isEmpty()) {
				continue;
			}
			if (i == QASlots.TRGSlotId) {
				String[] twords = slot.split(" ");
				String tverb = twords[twords.length - 1];
				String tpref = StrUtils.join(" ", twords, 0, twords.length - 1);
				if (!tpref.isEmpty()) {
					tpref += " ";
				}
				if (tverb.equals("do")) {
					slot = tpref + infl[0];
				} else if (tverb.equals("does")) {
					slot = tpref + infl[1];
				} else if (tverb.equals("doing")) {
					slot = tpref + infl[2];
				} else if (tverb.equals("did")) {
					slot = tpref + infl[3];
				} else {
					slot = tpref + infl[4];
				}
			}
			if (!qstr.isEmpty()) {
				qstr += " ";
			}
			qstr += slot;
		}
		return qstr;
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
