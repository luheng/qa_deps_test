package learning;

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
	
	int[][] o1Features;     // slot-id, option-id
	int[][][] o2Features;   // slot-id, option-id, prev-option-id
	int[][][][] o3Features; // slot-id, option-id, prev-option-id, prev...
	double[][] o1FeatVals;
	double[][][] o2FeatVals;
	double[][][][] o3FeatVals;
	
	public QuestionGenerationCRF(Corpus baseCorpus,
								 QuestionIdDataset trainSet,
								 HashMap<String, QuestionIdDataset> testSets) {
		this.baseCorpus = baseCorpus;
		this.trainSet = trainSet;
		this.testSets = testSets;
		initialize();
	}
	
	private void initialize() {
		featureExtractor = new QuestionGenFeatureExtractor(baseCorpus,
				minFeatureFreq);
		inflDict = ExperimentUtils.loadInflectionDictionary(baseCorpus);
		
		// ***************** Generate complete lattice for each sentence
		for (AnnotatedSentence sent : trainSet.sentences) {
			Sentence sentence = sent.sentence;
			for (int propHead : sent.qaLists.keySet()) {
				String[][] lattice = generateLattice(sentence, propHead);
				featureExtractor.extractFeatures(sentence, propHead, lattice,
						true /* accept new */);
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
