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
	
	private static int getAlignmentDistance(int[] align1, int[] align2) {
		int a1 = align1[0], b1 = align1[align1.length - 1],
			a2 = align2[0], b2 = align2[align2.length - 1]; 
		return Math.min(
				Math.min(Math.abs(a1 - a2), Math.abs(a1 - b2)),
			    Math.min(Math.abs(b1 - a2), Math.abs(b1 - b2)));
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
				ArrayList<QAPair> bufferedQAs = new ArrayList<QAPair>();
				
				String questionPP = result.questions.get(i)[5].trim();
				boolean extendAnswer = false;
				if (!questionPP.isEmpty() &&
					sentence.getTokensString().contains(questionPP)) {
					extendAnswer = true;
				}
				for (String answer : answers) {
					QAPair qa = new QAPair(
							question,
							extendAnswer ? questionPP + " " + answer : answer);
					qaAligner.align(sentence, qa);
					qa.setAlignedProposition(prop);
					bufferedQAs.add(qa);
				}
				if (bufferedQAs.size() == 1) {
					currSent.addQA(bufferedQAs.get(0));
					continue;
				}
				// Figure out the QA that contains the answer span that is
				// closest to the proposition.
				int minDist = sentence.length, mainQAId = -1;
				for (int j = 0; j < bufferedQAs.size(); j++) {
					QAPair qa = bufferedQAs.get(j);
					int spanDist = getAlignmentDistance(
							qa.answerAlignment,
							qa.propositionAlignment);
					if (spanDist < minDist) {
						minDist = spanDist;
						mainQAId = j;
					}
				}
			//	System.out.println("main qa:\t" + bufferedQAs.get(mainQAId).toString());
				for (int j = 0; j < bufferedQAs.size(); j++) {
					QAPair qa = bufferedQAs.get(j);
					if (j != mainQAId) {
						qa.mainQA = bufferedQAs.get(j);
			//			System.out.println("\t" + qa.toString());
						
					}
					currSent.addQA(qa);
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
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
