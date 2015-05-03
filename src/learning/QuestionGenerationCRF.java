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
	
	/*
	int[][] o1Features;     // slot-id, option-id
	int[][][] o2Features;   // slot-id, option-id, prev-option-id
	int[][][][] o3Features; // slot-id, option-id, prev-option-id, prev...
	double[][] o1FeatVals;
	double[][][] o2FeatVals;
	double[][][][] o3FeatVals;
	*/
	int[][][] latticeSizes; // instance-id, slot-id, 3
	
	int[][][][] featIds;     // instance-id, clique-id, feat-id
	double[][][][] featVals;
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
	
	private void initialize() {
		featureExtractor = new QuestionGenFeatureExtractor(baseCorpus,
				minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		
		// TODO: map sentence and target word to instance id.
		numInstances = 0;
		for (AnnotatedSentence sent : trainSet.sentences) {
			numInstances += sent.qaLists.size();
		}
		featIds = new int[numInstances][][][];
		featVals = new double[numInstances][][][];
		latticeSizes = new int[numInstances][][];
		
		int instId = 0;
		for (int sid = 0; sid < trainSet.sentences.size(); sid++) {
			AnnotatedSentence sent = trainSet.sentences.get(sid); 
			Sentence sentence = sent.sentence;
			for (int propHead : sent.qaLists.keySet()) {
				String[][] lattice = generateLattice(sentence, propHead);
				featIds[instId] = new int[lattice.length][][];
				featVals[instId] = new double[lattice.length][][];
				latticeSizes[instId] = new int[lattice.length][3];
				
				int numCliques = 1;
				int[] latIds = new int[3];
				for (int slotId = 0; slotId < lattice.length; slotId++) {
					for (int j = 0; j < 2; j++) {
						int k = slotId - 2 + j;
						latticeSizes[instId][slotId][j] =
								(k < 0 ? 1 : lattice[k].length);
						numCliques *= latticeSizes[instId][slotId][j];
					}
					featIds[instId][slotId] = new int[numCliques][];
					featVals[instId][slotId] = new double[numCliques][];
					// Pre-compute features
					int[] latSizes = latticeSizes[instId][slotId];
					for (latIds[0] = 0; latIds[0] < latSizes[0]; latIds[0]++) {
						for (latIds[1] = 0; latIds[1] < latSizes[1]; latIds[1]++) {
							for (latIds[2] = 0; latIds[2]< latSizes[2]; latIds[2]++) {
								featureExtractor.extractFeatures(
										sentence, propHead, lattice,
										slotId, latIds, true /* acceptNew */);
							}
						}
					}
				}
				instId ++;
			}
		}
		featureExtractor.freeze();
		
		instId = 0;
		for (int sid = 0; sid < trainSet.sentences.size(); sid++) {
			AnnotatedSentence sent = trainSet.sentences.get(sid); 
			Sentence sentence = sent.sentence;
			for (int propHead : sent.qaLists.keySet()) {
				String[][] lattice = generateLattice(sentence, propHead);
				int[] latIds = new int[3];
				for (int slotId = 0; slotId < lattice.length; slotId++) {
					int[] latSizes = latticeSizes[instId][slotId];
					for (latIds[0] = 0; latIds[0] < latSizes[0]; latIds[0]++) {
						for (latIds[1] = 0; latIds[1] < latSizes[1]; latIds[1]++) {
							for (latIds[2] = 0; latIds[2]< latSizes[2]; latIds[2]++) {
								TIntDoubleHashMap fv =
									featureExtractor.extractFeatures(
											sentence, propHead, lattice,
											slotId, latIds, false /* acceptNew */);
								int cliqueId = getCliqueId(latIds, latSizes);
								// TODO: populate feature vector
							}
						}
					}
				}
				instId ++;
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
	
}
