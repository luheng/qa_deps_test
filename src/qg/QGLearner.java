package qg;

import java.util.ArrayList;

import config.QuestionIdConfig;
import learning.QADataset;
import learning.QASample;
import util.StrUtils;
import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlots;
import data.Corpus;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;

public class QGLearner {
	protected Corpus baseCorpus;
	protected VerbInflectionDictionary inflDict;
	protected QuestionIdConfig config;
	
	public QGenDataset trainSet;
	public ArrayList<QGenDataset> testSets;
	public ArrayList<QGenSequence> sequences;
	public int numFeatures, numSequences, numTrains;
	
	protected QGenPotentialFunction potentialFunction;
	protected QGenFeatureExtractor featureExtractor;

	public QGLearner(Corpus corpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets, QuestionIdConfig config) {
		this.trainSet = trainSet;
		this.testSets = testSets;
		this.baseCorpus = corpus;
		this.config = config;
		initializeSequences("");
	}

	public QGLearner(Corpus corpus, QGenDataset trainSet,
			ArrayList<QGenDataset> testSets, QuestionIdConfig config,
			String qlabel) {
		this.trainSet = trainSet;
		this.testSets = testSets;
		this.baseCorpus = corpus;
		this.config = config;
		initializeSequences(qlabel);
	}
	
	protected void initializeSequences(String qlabel) {
		featureExtractor = new QGenFeatureExtractor(baseCorpus,
				config.minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		potentialFunction = new QGenPotentialFunction();
		
		sequences = new ArrayList<QGenSequence>();
		for (QASample sample : trainSet.samples) {
			if (!qlabel.isEmpty() && !sample.questionLabel.startsWith(qlabel)) {
				continue;
			}
			Sentence sentence = trainSet.sentenceMap.get(sample.sentenceId);
			sequences.add(initializeSequence(sentence, sample, true));
		}
		numTrains = sequences.size();
		for (QADataset testSet : testSets) {
			for (QASample sample : testSet.samples) {
				//if (!sample.questionLabel.startsWith(qlabel)) {
				//	continue;
				//}
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
	
	protected QGenSequence initializeSequence(Sentence sentence, QASample sample,
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
	
	protected String getGenericTrg(Sentence sent, int propHead, String aux,
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
