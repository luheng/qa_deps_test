package experiments;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import util.LatticeUtils;
import util.StringUtils;
import data.AnnotatedSentence;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;
import annotation.PropositionAligner;
import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerStage2DataRetriever {

	private static final String annotationFilePath =
			//   "crowdflower/CF_QA_trial_s20_result.csv";
			"crowdflower/cf_round1_100sents_stage2/f705896_CF_QA_s100_stage2_testrun.csv";
	
	public static void readAnnotationResult(
			ArrayList<CrowdFlowerQAResult> results) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		double avgTime = 0;
		int numRecords = 0;
		for (CSVRecord record : records) {
			// TODO: change this ...
			CrowdFlowerQAResult qa = CrowdFlowerQAResult.parseCSV(record);
			results.add(qa);
			avgTime += qa.secondsToComplete;
			++ numRecords;
		}
		fileReader.close();
		
		avgTime /= numRecords;
		System.out.println(String.format("Read %d CrowdFlower Records.", numRecords));
		System.out.println(String.format("Averaged completion time: %.3f seconds.",
				avgTime));
		System.out.println(String.format("Averaged number of units per hour: %.3f",
				3600 / avgTime));
	}
	
	public static void alignAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerQAResult> cfResults, SRLCorpus corpus) {
		assert (annotatedSentences != null);
		PropositionAligner propAligner = new PropositionAligner();

		// Map sentence IDs to 0 ... #sentences.
		Set<Integer> sentenceIds = new HashSet<Integer>();
		HashMap<Integer, Integer> sentIdMap = new HashMap<Integer, Integer>();
		for (CrowdFlowerQAResult result : cfResults) {
			sentenceIds.add(result.sentenceId);
		}
		for (int id : sentenceIds) {
			annotatedSentences.add(new AnnotatedSentence(
					(SRLSentence) corpus.sentences.get(id)));
			sentIdMap.put(id, annotatedSentences.size() - 1);
		}
		
		for (CrowdFlowerQAResult result : cfResults) {
			int sentId = result.sentenceId;
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			AnnotatedSentence currSent =
					annotatedSentences.get(sentIdMap.get(result.sentenceId));
			
			int propHead = -1;
			if (result.propStart == -1) {
				Proposition prop = propAligner.align(sentence,
						result.proposition);
				propHead = prop.span[1] - 1;
			} else {
				propHead = result.propEnd - 1;
			}
			currSent.addProposition(propHead);
		
			/* todo here */
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		ArrayList<CrowdFlowerQAResult> annotationResults =
				new ArrayList<CrowdFlowerQAResult>();
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		
		try {
			readAnnotationResult(annotationResults);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		alignAnnotations(annotatedSentences, annotationResults, trainCorpus);
	}
}
