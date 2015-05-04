package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;
import io.XSSFOutputHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotQuestionWords;
import data.AnnotatedSentence;
import data.Corpus;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;

public class QuestionGenerationCRF {
	
	private Corpus baseCorpus; 
	private VerbInflectionDictionary inflDict;
	private QuestionIdDataset trainSet;
	private HashMap<String, QuestionIdDataset> testSets;
	
	private static int minFeatureFreq = 5;
	
	QuestionGenFeatureExtractor featureExtractor;
	int[][][] latticeSizes; // instance-id, slot-id, 3
	
	int[][][][] featIds;     // instance-id, clique-id, feat-id
	double[][][][] featVals;
	ArrayList<Instance> instances;
	int numInstances;
	
	public QuestionGenerationCRF(Corpus baseCorpus,
								 QuestionIdDataset trainSet,
								 HashMap<String, QuestionIdDataset> testSets) {
		this.baseCorpus = baseCorpus;
		this.trainSet = trainSet;
		this.testSets = testSets;
		initialize();
	}
	
	/**
	 * For example, [0, 0, 5] , [1, 1, 10] -> (1 * 10 * 0) + (10 * 0) + 5
	 * For example, [2, 3, 5] , [3, 5, 10] -> (5 * 10 * 2) + (10 * 3) + 5
	 * @param latticeIds
	 * @param latticeSizes
	 * @return
	 */
	private static int getCliqueId(int[] latticeIds, int[] latticeSizes) {
		int step = 1, cliqueId = 0;
		for (int i = latticeIds.length - 1; i >= 0; i--) {
			cliqueId += step * latticeIds[i];
			step *= latticeSizes[i];
		}
		return cliqueId;
	}
	
	private static int[] getLatticeIds(int cliqueId, int[] latticeSizes) {
		int step = 1, rem = cliqueId;
		int[] latticeIds = new int[latticeSizes.length];
		for (int i = 1; i < latticeSizes.length; i++) {
			step *= latticeSizes[i];
		}
		for (int i = 0; i < latticeSizes.length; i++) {
			latticeIds[i] = rem / step;
			rem %= step;
			if (i + 1 < latticeSizes.length) {
				step /= latticeSizes[i + 1];
			}
		}
		return latticeIds;
	}
	
	private void populateFeatureVector(int instId, int slotId, int cliqueId,
			TIntDoubleHashMap fv) {
		int[] fids = Arrays.copyOf(fv.keys(), fv.size());
		Arrays.sort(fids);
		featIds[instId][slotId][cliqueId] = new int[fids.length];
		featVals[instId][slotId][cliqueId] = new double[fids.length];
		for (int i = 0; i < fids.length; i++) {
			// Liblinear feature id starts from 1.
			featIds[instId][slotId][cliqueId][i] = fids[i];
			featVals[instId][slotId][cliqueId][i] = fv.get(fids[i]);
		}
	}
	
	private void initialize() {
		featureExtractor = new QuestionGenFeatureExtractor(baseCorpus,
				minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		
		instances = new ArrayList<Instance>();
		
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				instances.add(new Instance(sent, propHead, true));
			}
		}
		for (QuestionIdDataset testSet : testSets.values()) {
			for (AnnotatedSentence sent : testSet.sentences) {
				for (int propHead : sent.qaLists.keySet()) {
					instances.add(new Instance(sent, propHead, false));
				}
			}
		}
		numInstances = instances.size();
		System.out.println(String.format("Processing %d instances.",
				numInstances));
		featIds = new int[numInstances][][][];
		featVals = new double[numInstances][][][];
		latticeSizes = new int[numInstances][][];
		
		for (int instId = 0; instId < numInstances; instId ++) {
			Instance inst = instances.get(instId);
			Sentence sentence = inst.sentence.sentence;
			int propHead = inst.propHead;
			
			String[][] lattice = generateLattice(sentence, propHead);
			featIds[instId] = new int[lattice.length][][];
			featVals[instId] = new double[lattice.length][][];
			latticeSizes[instId] = new int[lattice.length][3];
			for (int slotId = 0; slotId < lattice.length; slotId++) {
				int numCliques = 1;
				for (int j = 0; j < 3; j++) {
					int k = slotId - 2 + j;
					latticeSizes[instId][slotId][j] =
							(k < 0 ? 1 : lattice[k].length);
					numCliques *= latticeSizes[instId][slotId][j];
				}
				featIds[instId][slotId] = new int[numCliques][];
				featVals[instId][slotId] = new double[numCliques][];
				int[] ranges = latticeSizes[instId][slotId];
				int[] ids = new int[3];
				for (ids[0] = 0; ids[0] < ranges[0]; ids[0]++) {
					for (ids[1] = 0; ids[1] < ranges[1]; ids[1]++) {
						for (ids[2] = 0; ids[2]< ranges[2]; ids[2]++) {
							featureExtractor.extractFeatures(
									sentence, propHead, lattice,
									slotId, ids, true /* acceptNew */);
						}
					}
				}
			}
		}
		featureExtractor.freeze();
		System.out.println(String.format("Extracted %d features.",
				featureExtractor.featureDict.size()));
		for (int instId = 0; instId < numInstances; instId ++) {
			Instance inst = instances.get(instId);
			Sentence sentence = inst.sentence.sentence;
			int propHead = inst.propHead;
			String[][] lattice = generateLattice(sentence, propHead);
			for (int slotId = 0; slotId < lattice.length; slotId++) {
				int[] ranges = latticeSizes[instId][slotId];
				int[] ids = new int[3];
				for (ids[0] = 0; ids[0] < ranges[0]; ids[0]++) {
					for (ids[1] = 0; ids[1] < ranges[1]; ids[1]++) {
						for (ids[2] = 0; ids[2]< ranges[2]; ids[2]++) {
							TIntDoubleHashMap fv =
								featureExtractor.extractFeatures(
										sentence, propHead, lattice,
										slotId, ids, false /* acceptNew */);
							int cliqueId = getCliqueId(ids, ranges);
							populateFeatureVector(instId, slotId, cliqueId, fv);
						}
					}
				}
			}
		}
	}
	
	private String[][] generateLattice(Sentence sentence, int propHead) {
		String[][] lattice = new String[7][];
		// 1. WH
		lattice[0] = Arrays.copyOf(QASlotQuestionWords.values,
								   QASlotQuestionWords.values.length);
		// 2. AUX
		lattice[1] = Arrays.copyOf(QASlotAuxiliaryVerbs.values,
				   				   QASlotAuxiliaryVerbs.values.length);
		// 3. PH1
		lattice[2] = Arrays.copyOf(QASlotPlaceHolders.values,
								   QASlotPlaceHolders.values.length);
		// 4. TRG
		ArrayList<String> trgOptions = XSSFOutputHelper.getTrgOptions(
				sentence, propHead, inflDict);
		lattice[3] = trgOptions.toArray(new String[trgOptions.size()]);
		// 5. PH2
		lattice[4] = Arrays.copyOf(QASlotPlaceHolders.values,
								   QASlotPlaceHolders.values.length);
		// 6. PP
		ArrayList<String> ppOptions = XSSFOutputHelper.getPPOptions(sentence);
		lattice[5] = ppOptions.toArray(new String[ppOptions.size()]);
		// 7. PH3
		lattice[6] = Arrays.copyOf(QASlotPlaceHolders.ph3Values,
				   				   QASlotPlaceHolders.ph3Values.length);
		return lattice;
	}
	
	private class Instance {
		AnnotatedSentence sentence;
		int propHead;
		boolean isTraining;
		Instance(AnnotatedSentence sentence, int propHead, boolean isTraining) {
			this.sentence = sentence;
			this.propHead = propHead;
			this.isTraining = isTraining;
		}
	}
	
}
