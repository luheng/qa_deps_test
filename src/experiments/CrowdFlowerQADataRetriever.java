package experiments;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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

public class CrowdFlowerQADataRetriever {

	private static final String annotationFilePath =
			//   "crowdflower/CF_QA_trial_s20_result.csv";
			"crowdflower/cf_round1_100sents_259units/f680088_CF_QA_s100_final_results.csv";
	
	public static void readAnnotationResult(
			ArrayList<CrowdFlowerQAResult> results) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		double avgTime = 0;
		int numRecords = 0;
		for (CSVRecord record : records) {
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
			for (int i = 0; i < result.questions.size(); i++) {
				String[] question = result.questions.get(i);				
				if (question[0].equalsIgnoreCase("how many")) {
					continue;
				}
				String[] answers = result.answers.get(i);
				StructuredQAPair qa = new StructuredQAPair(sentence, propHead,
						question, answers[0], result);
				for (int j = 1; j < answers.length; j++) {
					qa.addAnswer(answers[j]);
				}
				currSent.addQAPair(propHead, qa);
			}
			// TODO: look at people's feedback
			/*
			if (!result.feedback.isEmpty()) {
				System.out.println(sentence.getTokensString());
				System.out.println("Prop:\t" + sentence.getTokenString(propHead));
				System.out.println("Feedback:\t" + result.feedback);
				for (int i = 0; i < result.questions.size(); i++) {
					System.out.println("\t" + StringUtils.join(result.questions.get(i), " "));
					System.out.println("\t" + StringUtils.join(result.answers.get(i), " / "));
				}
				System.out.println();
			}
			*/
		}
	}
	
	static void aggregateAnnotationsByQuestion(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		int numQAs = 0,
			numAggregatedQAs = 0;
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				ArrayList<StructuredQAPair> qaList =
						annotSent.qaLists.get(propHead);
				HashMap<String, Integer> qmap = new HashMap<String, Integer>();
				
				for (StructuredQAPair qa : qaList) {
					String qstr = qa.getQuestionLabel();
					int k = (qmap.containsKey(qstr) ? qmap.get(qstr) : 0);
					qmap.put(qstr, k + 1);
				}
				ArrayList<StructuredQAPair> newList =
						new ArrayList<StructuredQAPair>();
				
				for (String qlabel : qmap.keySet()) {
					if (qmap.get(qlabel) <= 1 || qlabel.contains("???")) {
						continue;
					}
					StructuredQAPair newQA = new StructuredQAPair(
							annotSent.sentence, propHead, qlabel,
							"" /* answer */,
							null /* cfAnnotationResult */);
					for (StructuredQAPair qa : qaList) {
						if (qa.getQuestionLabel().equals(qlabel)) {
							newQA.addAnswer(qa.answerFlags);
							//newQA.addAnnotationSource(qa.cfAnnotationSources.get(0));
						}
					}
					newList.add(newQA);
				}
				numQAs += annotSent.qaLists.get(propHead).size();
				numAggregatedQAs += newList.size();
				annotSent.qaLists.put(propHead, newList);
			}
		}
		System.out.println("Num QAs before filtering:\t" + numQAs +
		 		   		   "\nNum QAs after filtering:\t" + numAggregatedQAs);
	}
	
	static void checkDistinctQuestionLabels(SRLCorpus corpus,
			ArrayList<CrowdFlowerQAResult> results) {
		HashMap<String, String> qmap = new HashMap<String, String>(),
								amap = new HashMap<String, String>();
		int numQLabels = 0,
			numCollided = 0;
		for (CrowdFlowerQAResult result : results) {
			for (int i = 0; i < result.questions.size(); i++) {
				DepSentence sentence = corpus.sentences.get(result.sentenceId);
				String[] question = result.questions.get(i);
				String keyStr = String.format("%d_%d_%d_%s",
						result.sentenceId, result.propEnd - 1,
						result.cfWorkerId, QuestionEncoder.encode(question, sentence));
				String aStr = StringUtils.join(" ... ", result.answers.get(i)); 
				String qaStr = StringUtils.join(" ", question) + "?\t" + aStr;
						
				if (qmap.containsKey(keyStr) && !amap.get(keyStr).equals(aStr)) {
					System.out.println(keyStr);
					System.out.println("\t" + sentence.getTokensString());
					System.out.println("\t" + qmap.get(keyStr));
					System.out.println("\t" + qaStr);
					System.out.println();
					numCollided++;
				}
				numQLabels ++;
				qmap.put(keyStr, qaStr);
				amap.put(keyStr, aStr);
			}
		}
		System.out.println(numQLabels + ", " + numCollided);
	}
	
	static void computeInterAnnotatorAgreement(SRLCorpus corpus,
			ArrayList<CrowdFlowerQAResult> results) {
		HashMap<String, ArrayList<StructuredQAPair>> qaMap =
				new HashMap<String, ArrayList<StructuredQAPair>>();
		
		for (CrowdFlowerQAResult result : results) {
			int sentId = result.sentenceId,
				propId = result.propEnd - 1;
			String sentTrgKey = String.format("%d_%d", sentId, propId);
			
			if (!qaMap.containsKey(sentTrgKey)) {
				qaMap.put(sentTrgKey, new ArrayList<StructuredQAPair>());
			}
			HashMap<String, StructuredQAPair> qmap =
					new HashMap<String, StructuredQAPair>();
			// Aggregate each worker's results by question label.
			for (int i = 0; i < result.questions.size(); i++) {
				DepSentence sentence = corpus.sentences.get(result.sentenceId);
				StructuredQAPair qa = new StructuredQAPair(
						(SRLSentence) sentence, propId, result.questions.get(i),
						"" /* ansewr */, result);
				for (String answer : result.answers.get(i)) {
					qa.addAnswer(answer);
				}
				String qlabel = qa.getQuestionLabel();
				if (qmap.containsKey(qlabel)) {
					qmap.get(qlabel).addAnswer(qa.answerFlags);
					qmap.get(qlabel).addAnnotationSource(result);
				} else {
					qmap.put(qlabel, qa);
				}
			}
			for (String qlabel : qmap.keySet()) {
				qaMap.get(sentTrgKey).add(qmap.get(qlabel));
			}
		}
		
		// Compute inter-annotator agreement
		// <Q1, A1> \equals <Q2, A2> iff:
		//   (1) label(Q1) == label(Q2)
		//   (2) answer(A1) = answer(A2)
		double avgTwoAgreement = .0, avgThreeAgreement = .0;
		for (String sentTrgKey : qaMap.keySet()) {
			int totalQAs = qaMap.get(sentTrgKey).size(),
				twoAgreedQAs = 0,
				threeAgreedQAs = 0;
			HashMap<String, int[]> qaCount = new HashMap<String, int[]>();
			for (StructuredQAPair qa : qaMap.get(sentTrgKey)) {
				int sentLength = qa.answerFlags.length;
				String qlabel = qa.questionLabel;
				if (!qaCount.containsKey(qlabel)) {
					qaCount.put(qlabel, new int[sentLength]);
					Arrays.fill(qaCount.get(qlabel), 0);
				}
				int[] flags = qaCount.get(qlabel);
				for (int i = 0; i < sentLength; i++) {
					flags[i] += (qa.answerFlags[i] > 0 ? 1 : 0);
				}
				System.out.println(qa.cfAnnotationSources.get(0).cfWorkerId +
						"\t" + qa.toString());
			}
			for (String qlabel : qaCount.keySet()) {
				int maxCount = 0;
				int[] flags = qaCount.get(qlabel);
				for (int i = 0; i < flags.length; i++) {
					maxCount = Math.max(maxCount, flags[i]);
				}
				twoAgreedQAs += (maxCount >= 2 ? 1 : 0);
				threeAgreedQAs += (maxCount >= 3 ? 1 : 0);
			}
			
			System.out.println(String.format("%s\t%d\t%d\t%d\t%.3f\t%.3f",
					sentTrgKey,
					totalQAs, twoAgreedQAs, threeAgreedQAs,
					1.0 * twoAgreedQAs / totalQAs,
					1.0 * threeAgreedQAs / totalQAs));
			avgTwoAgreement += 1.0 * twoAgreedQAs / totalQAs;
			avgThreeAgreement += 1.0 * threeAgreedQAs / totalQAs;
		}
		System.out.println(avgTwoAgreement / qaMap.size());
		System.out.println(avgThreeAgreement / qaMap.size());
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
		//aggregateAnnotationsByQuestion(annotatedSentences);
		//checkDistinctQuestionLabels(trainCorpus, annotationResults);
	
		/*
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
		}*/
		
		computeInterAnnotatorAgreement(trainCorpus, annotationResults);
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		//tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
