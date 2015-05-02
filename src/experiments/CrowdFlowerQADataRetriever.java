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
import data.QAPair;
import annotation.CrowdFlowerQAResult;
import annotation.PropositionAligner;
import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;

public class CrowdFlowerQADataRetriever {

	private static final String annotationFilePath =
			//   "crowdflower/CF_QA_trial_s20_result.csv";
			//"crowdflower/cf_round1_100sents_259units/f680088_CF_QA_s100_final_results.csv";
			"crowdflower/cf_round2_100sents/cf_qa_r2_test_f708517_4.csv";
	
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
			int propHead = result.getPropHead();
			currSent.addProposition(propHead);
			
			for (int i = 0; i < result.questions.size(); i++) {
				String[] question = result.questions.get(i);				
				if (question[0].equalsIgnoreCase("how many")) {
					continue;
				}
				QAPair qa = new QAPair(
						sentence,
						propHead,
						question,
						"" /* answer */,
						result);
				for (String answer : result.answers.get(i)) {
					qa.addAnswer(answer);
				}
				currSent.addQAPair(propHead, qa);
			}
			// Look at feedback
			if (!result.feedback.isEmpty()) {
				System.out.println(result.cfWorkerId);
				System.out.println(sentence.getTokensString());
				System.out.println("Prop:\t" + sentence.getTokenString(propHead));
				System.out.println("Feedback:\t" + result.feedback);
				for (int i = 0; i < result.questions.size(); i++) {
					System.out.println("\t" + StringUtils.join(" ", result.questions.get(i)));
					System.out.println("\t" + StringUtils.join(" / ", result.answers.get(i)));
				}
				System.out.println();
			}
		}
	}
	
	static void aggregateAnnotationsByQuestion(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		int numQAs = 0,
			numAggregatedQAs = 0;
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				ArrayList<QAPair> qaList =
						annotSent.qaLists.get(propHead);
				HashMap<String, Integer> qmap = new HashMap<String, Integer>();
				
				for (QAPair qa : qaList) {
					String qstr = qa.getQuestionLabel();
					int k = (qmap.containsKey(qstr) ? qmap.get(qstr) : 0);
					qmap.put(qstr, k + 1);
				}
				ArrayList<QAPair> newList =
						new ArrayList<QAPair>();
				
				for (String qlabel : qmap.keySet()) {
					// Remove unagreed or bad question labels.
					if (qmap.get(qlabel) <= 1 || qlabel.contains("???")) {
						continue;
					}
					QAPair newQA = new QAPair(
							annotSent.sentence,
							propHead,
							qlabel,
							"" /* answer */,
							null /* cfAnnotationResult */);
					for (QAPair qa : qaList) {
						if (qa.getQuestionLabel().equals(qlabel)) {
							newQA.addAnswer(qa.answerFlags);
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
				DepSentence sentence = corpus.getSentence(result.sentenceId);
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
		HashMap<String, ArrayList<QAPair>> qaMap =
				new HashMap<String, ArrayList<QAPair>>();
		
		for (CrowdFlowerQAResult result : results) {
			int sentId = result.sentenceId,
				propId = result.propEnd - 1;
			String sentTrgKey = String.format("%d_%d", sentId, propId);
			
			if (!qaMap.containsKey(sentTrgKey)) {
				qaMap.put(sentTrgKey, new ArrayList<QAPair>());
			}
			HashMap<String, QAPair> qmap =
					new HashMap<String, QAPair>();
			// Aggregate each worker's results by question label.
			for (int i = 0; i < result.questions.size(); i++) {
				DepSentence sentence = corpus.getSentence(result.sentenceId);
				QAPair qa = new QAPair(
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
			/*
			int totalQAs = 0,
				twoAgreedQAs = 0,
				threeAgreedQAs = 0;
			*/
			HashMap<String, int[]> qaCount = new HashMap<String, int[]>();
			TIntIntHashMap workerCount = new TIntIntHashMap(),
						   workerTwoAgreed = new TIntIntHashMap(),
						   workerThreeAgreed = new TIntIntHashMap();
			
			for (QAPair qa : qaMap.get(sentTrgKey)) {
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
			}
			for (QAPair qa : qaMap.get(sentTrgKey)) {
				int workerId = qa.cfAnnotationSources.get(0).cfWorkerId;
				int maxCount = 0;
				int[] flags = qaCount.get(qa.questionLabel);
				for (int i = 0; i < qa.answerFlags.length; i++) {
					if (qa.answerFlags[i] > 0) {
						maxCount = Math.max(maxCount, flags[i]);
					}
				}
				if (maxCount >= 2) {
					workerTwoAgreed.adjustOrPutValue(workerId, 1, 1);
				}
				if (maxCount >= 3) {
					workerThreeAgreed.adjustOrPutValue(workerId, 1, 1);
				}
				workerCount.adjustOrPutValue(workerId, 1, 1);
			}
			/*
			for (String qlabel : qaCount.keySet()) {
				int maxCount = 0;
				int[] flags = qaCount.get(qlabel);
				for (int i = 0; i < flags.length; i++) {
					maxCount = Math.max(maxCount, flags[i]);
				}
				twoAgreedQAs += (maxCount >= 2 ? 1 : 0);
				threeAgreedQAs += (maxCount >= 3 ? 1 : 0);
				totalQAs += (3 - maxCount + 1);
			}
			*/
			
			double avgTwoAccuracy = 0.0, avgThreeAccuracy = 0.0;
			for (int workerId : workerCount.keys()) {
				int cnt = workerCount.get(workerId);
				double twoAcc = 1.0 * workerTwoAgreed.get(workerId) / cnt;
				double threeAcc = 1.0 * workerThreeAgreed.get(workerId) / cnt;
				avgTwoAccuracy += twoAcc;
				avgThreeAccuracy += threeAcc;
			}
			avgTwoAgreement += 1.0 * avgTwoAccuracy / workerCount.keys().length;
			avgThreeAgreement += 1.0 * avgThreeAccuracy / workerCount.keys().length;
			/*
			System.out.println(String.format("%s\t%d\t%d\t%d\t%.3f\t%.3f",
					sentTrgKey,
					totalQAs, twoAgreedQAs, threeAgreedQAs,
					1.0 * twoAgreedQAs / totalQAs,
					1.0 * threeAgreedQAs / totalQAs));
			avgTwoAgreement += 1.0 * twoAgreedQAs / totalQAs;
			avgThreeAgreement += 1.0 * threeAgreedQAs / totalQAs;
			*/
		}
		System.out.println(avgTwoAgreement / qaMap.size());
		System.out.println(avgThreeAgreement / qaMap.size());
	}
	
	private static void debugOutput(SRLCorpus corpus,
			ArrayList<AnnotatedSentence> annotatedSentences) {
		// TODO: Print Precision/recall/F1
		// TODO: Print sentence 10-by-10
		SRLAnnotationValidator validator = new SRLAnnotationValidator();
		validator.ignoreLabels = true;
		int sentCount = 0;
		
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("debug_r2.xls"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for (AnnotatedSentence annotSent : annotatedSentences) {
			sentCount ++;
			SRLSentence sentence = (SRLSentence) annotSent.sentence;
			String[][] gold = validator.getGoldSRL(sentence);
			int[][] covered = new int[gold.length][];
			for (int i = 0; i < gold.length; i++) {
				covered[i] = new int[gold[i].length];
			}
			LatticeUtils.fill(covered, 0);
			int sentLength = sentence.length;
			for (Proposition prop : sentence.propositions) {
				int propId = prop.propID;
				if (!annotSent.qaLists.containsKey(propId)) {
					continue;
				}
				//System.out.println(annotSent.sentence.getTokenString(propId));
				// Compute Agreement.
				HashMap<String, int[]> qaCount = new HashMap<String, int[]>();
				for (QAPair qa : annotSent.qaLists.get(propId)) {
					String qlabel = qa.questionLabel;
					if (!qaCount.containsKey(qlabel)) {
						qaCount.put(qlabel, new int[sentLength]);
						Arrays.fill(qaCount.get(qlabel), 0);
					}
					int[] flags = qaCount.get(qlabel);
					for (int i = 0; i < sentLength; i++) {
						flags[i] += (qa.answerFlags[i] > 0 ? 1 : 0);
						if (!gold[propId + 1][i + 1].isEmpty() &&
							validator.matchedGold(i, qa, sentence)) {
							covered[propId + 1][i + 1] = 1;
						}
					}
				}
				System.out.println(
						sentence.sentenceID + "\t" +
						sentence.getTokensString());
				System.out.println(
						sentence.getTokenString(propId) + "\t" +
						corpus.propDict.getString(prop.propType));
				for (int i = 0; i < prop.argIDs.size(); i++) {
					int argTypeId = prop.argTypes.get(i),
						argId = prop.argIDs.get(i);
					String argType = corpus.argModDict.getString(argTypeId);
					if (argType.equals("AM-NEG") ||
						argType.equals("AM-MOD") ||
						argType.startsWith("R-")) {
						continue;
					}
					System.out.println(
							" \t" +
							argType + "\t" +
							" \t" +
							sentence.getTokenString(argId) + "\t" +
							(covered[propId + 1][argId + 1] > 0 ? " " : "NC"));
				}
				for (QAPair qa : annotSent.qaLists.get(propId)) {
					int workerId = qa.cfAnnotationSources.get(0).cfWorkerId;
					String qlabel = qa.getQuestionLabel();
					boolean agreed = false,
							matched = false;
					int[] flags = qaCount.get(qlabel);
					for (int i = 0; i < sentLength; i++) {
						if (qa.answerFlags[i] > 0 && flags[i] > 1) {
							agreed = true;
						}
						if (!gold[propId + 1][i + 1].isEmpty() &&
							validator.matchedGold(i, qa, sentence)) {
							matched = true;
						}
					}
					System.out.println(
							workerId + "\t" +
							qa.questionLabel + "\t" +
							qa.getQuestionString() + "\t" +
							qa.getAnswerString() + "\t" + 
							(agreed ? " " : "NA") + "\t" +
							(matched ? " " : "NG") + "\t");
				}
				System.out.println();
			}
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
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
		debugOutput(trainCorpus, annotatedSentences);
		aggregateAnnotationsByQuestion(annotatedSentences);
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
		
		//computeInterAnnotatorAgreement(trainCorpus, annotationResults);
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		//tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences, trainCorpus);
	}
}
