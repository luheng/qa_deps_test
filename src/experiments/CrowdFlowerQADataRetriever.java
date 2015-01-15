package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import util.StringUtils;
import data.AnnotatedSentence;
import data.Proposition;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;
import annotation.CrowdFlowerQAResult;
import annotation.DistanceSensitiveQuestionAnswerAligner;
import annotation.GreedyQuestionAnswerAligner;
import annotation.PropositionAligner;

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
			//System.out.println(qa.toString());
		}
		fileReader.close();
	}
	
	public static void alignAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences,
			ArrayList<CrowdFlowerQAResult> cfResults, SRLCorpus corpus) {
		assert (annotatedSentences != null);
		
		PropositionAligner propAligner = new PropositionAligner();
		DistanceSensitiveQuestionAnswerAligner qaAligner =
				new DistanceSensitiveQuestionAnswerAligner();
		
		// Get a list of sentences.
		Set<Integer> sentenceIds = new HashSet<Integer>();
		HashMap<Integer, Integer> sentIdMap = new HashMap<Integer, Integer>();
		for (CrowdFlowerQAResult result : cfResults) {
			sentenceIds.add(result.sentenceId);
		}
		annotatedSentences = new ArrayList<AnnotatedSentence>();
		for (int id : sentenceIds) {
			annotatedSentences.add(new AnnotatedSentence(
					corpus.sentences.get(id)));
			sentIdMap.put(id, annotatedSentences.size() - 1);
		}
		
		for (CrowdFlowerQAResult result : cfResults) {
			int sentId = result.sentenceId;
			
			SRLSentence sentence = (SRLSentence) corpus.sentences
					.get(sentId);
			
			// assume the propositions are unique, for now
			AnnotatedSentence currSent = annotatedSentences.get(
					sentIdMap.get(result.sentenceId));
			Proposition prop = propAligner.align(sentence, result.proposition);
			for (int i = 0; i < result.questions.size(); i++) {
				String question = StringUtils.join(" ", result.questions.get(i));
				String[] answers = result.answers.get(i);
				for (String answer : answers) {
					QAPair qa = new QAPair(question, answer);
					qaAligner.align(sentence, qa);
					qa.setAlignedProposition(prop);
					currSent.addQA(qa);
				}
			}
		}
		
		for (AnnotatedSentence sent : annotatedSentences) {
			sent.prettyPrintAlignment();
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
