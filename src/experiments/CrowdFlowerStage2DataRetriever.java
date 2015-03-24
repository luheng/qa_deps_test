package experiments;

import gnu.trove.list.array.TIntArrayList;

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
import data.SRLCorpus;
import data.SRLSentence;
import annotation.CrowdFlowerQAResult;
import annotation.CrowdFlowerStage2Result;
import annotation.PropositionAligner;

public class CrowdFlowerStage2DataRetriever {

	private static final String annotationFilePath =
			//   "crowdflower/CF_QA_trial_s20_result.csv";
			"crowdflower/cf_round1_100sents_stage2/f705896_CF_QA_s100_stage2_testrun.csv";
	
	public static void readAnnotationResult(
			ArrayList<CrowdFlowerStage2Result> results) throws IOException {
		assert (results != null);
		
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		double avgTime = 0;
		int numRecords = 0;
		for (CSVRecord record : records) {
			CrowdFlowerStage2Result qa = CrowdFlowerStage2Result.parseCSV(record);
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
	
	public static void analyzeAnnotations(
			ArrayList<CrowdFlowerQAResult> stage1Results,
			ArrayList<CrowdFlowerStage2Result> stage2Results,
			SRLCorpus corpus) {
		// Aggregate Stage1 results.
		HashMap<String, ArrayList<CrowdFlowerQAResult>> stage1ResultsMap =
				new HashMap<String, ArrayList<CrowdFlowerQAResult>>();
		for (CrowdFlowerQAResult result : stage1Results) {
			int sentId = result.sentenceId,
				propHead = result.propEnd - 1;
			String cfKey = String.format("%d_%d", sentId, propHead);
			if (!stage1ResultsMap.containsKey(cfKey)) {
				stage1ResultsMap.put(cfKey, new ArrayList<CrowdFlowerQAResult>());
			}
			stage1ResultsMap.get(cfKey).add(result);
		}
		// Compare with Stage1 results.
		for (CrowdFlowerStage2Result result : stage2Results) {
			int sentId = result.sentenceId;
			SRLSentence sentence = (SRLSentence) corpus.sentences.get(sentId);
			int propHead = result.propEnd - 1;
		
			// Retrieve Stage1 result
			String cfKey = String.format("%d_%d", sentId, propHead);
			
			for (CrowdFlowerQAResult stage1Result : stage1ResultsMap.get(cfKey)) {
				//TODO: debug the worker ID problem.
				System.out.println(result.stage1Id + "..." + stage1Result.cfWorkerId); 
				
				for (int qid = 0; qid < stage1Result.questions.size(); qid++) {
					String[] question = stage1Result.questions.get(qid);
					String qstr = StringUtils.join(" ", question).trim();
					if (!qstr.equalsIgnoreCase(result.question)) {
						continue;
					}
					// Compare.					
					System.out.println(qstr);
					System.out.println("==== stage1 answers ====");
					System.out.println(StringUtils.join("\n", stage1Result.answers.get(qid)));
					System.out.println("==== stage2 answers ====");
					System.out.println(StringUtils.join("\n", result.answers));
					System.out.println();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		
		ArrayList<CrowdFlowerQAResult> stage1Results =
				new ArrayList<CrowdFlowerQAResult>();
		ArrayList<CrowdFlowerStage2Result> stage2Results =
				new ArrayList<CrowdFlowerStage2Result>();
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		
		try {
			CrowdFlowerQADataRetriever.readAnnotationResult(stage1Results);
			readAnnotationResult(stage2Results);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		CrowdFlowerQADataRetriever.alignAnnotations(annotatedSentences,
				stage1Results, trainCorpus);
		analyzeAnnotations(stage1Results, stage2Results, trainCorpus);
	}
}
