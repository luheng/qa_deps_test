package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import data.AnnotatedSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;
import annotation.PropositionAligner;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataRetriever {

	private static final String annotationFilePath =
			"crowdflower/CF_QA_trial_s20_result.csv";
	
	public static void readAnnotationResult(
			ArrayList<CrowdFlowerQAResult> results) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		for (CSVRecord record : records) {
			CrowdFlowerQAResult qa = CrowdFlowerQAResult.parseCSV(record);
			results.add(qa);
		}
		fileReader.close();
	}
	
	/*
	private static int getAlignmentDistance(int[] align1, int[] align2) {
		int a1 = align1[0], b1 = align1[align1.length - 1],
			a2 = align2[0], b2 = align2[align2.length - 1]; 
		return Math.min(
				Math.min(Math.abs(a1 - a2), Math.abs(a1 - b2)),
			    Math.min(Math.abs(b1 - a2), Math.abs(b1 - b2)));
	}
	*/
	
	public static void alignAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerQAResult> cfResults, SRLCorpus corpus) {
		assert (annotatedSentences != null);
		PropositionAligner propAligner = new PropositionAligner();
		
		// Get a list of sentences.
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
			SRLSentence sentence = (SRLSentence) corpus.sentences
					.get(sentId);
			AnnotatedSentence currSent = annotatedSentences.get(
					sentIdMap.get(result.sentenceId));
			// Assume the propositions are unique, for now.
			Proposition prop = propAligner.align(sentence, result.proposition);
			int propHead = prop.span[1] - 1;
			currSent.addProposition(propHead);
			for (int i = 0; i < result.questions.size(); i++) {
				String[] question = result.questions.get(i);
				String[] answers = result.answers.get(i);	
				for (String answer : answers) {
					StructuredQAPair qa = new StructuredQAPair(sentence,
							propHead, question, answer, result);
					currSent.addQAPair(propHead, qa);
				}
			}
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
		for (AnnotatedSentence sent : annotatedSentences) {
			//System.out.println(sent.toString());
			for (int propId : sent.qaLists.keySet()) {
				System.out.println(sent.sentence.getTokenString(propId));
				ArrayList<StructuredQAPair> qaList = sent.qaLists.get(propId);
				for (StructuredQAPair qa : qaList) {
					System.out.println(qa.toString());
				}
			}
			System.out.println();
		}
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
