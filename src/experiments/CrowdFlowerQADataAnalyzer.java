package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import data.AnnotatedSentence;
import data.SRLCorpus;
import data.StructuredQAPair;
import annotation.CrowdFlowerQAResult;

public class CrowdFlowerQADataAnalyzer {

	static void aggregateAnnotations(
			ArrayList<AnnotatedSentence> annotatedSentences) {
		for (AnnotatedSentence annotSent : annotatedSentences) {			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, Integer> aggregatedQAMap =
						new HashMap<String, Integer>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedAnswer = qa.getAnswerString();
					if (!aggregatedQAMap.containsKey(encodedAnswer)) {
						aggregatedQAMap.put(encodedAnswer, 1);
					} else {
						int k = aggregatedQAMap.get(encodedAnswer);
						aggregatedQAMap.put(encodedAnswer, k + 1);
					}
				}
				ArrayList<StructuredQAPair> agreedList =
						new ArrayList<StructuredQAPair>();
				for (StructuredQAPair qa : annotSent.qaLists.get(propHead)) {
					String encodedAnswer = qa.getAnswerString();
					if (aggregatedQAMap.get(encodedAnswer) > 1) {
						agreedList.add(qa);
						aggregatedQAMap.put(encodedAnswer, 0);
					}
				}
				annotSent.qaLists.put(propHead, agreedList);
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
			CrowdFlowerQADataRetriever.readAnnotationResult(annotationResults);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		CrowdFlowerQADataRetriever.alignAnnotations(annotatedSentences,
				annotationResults, trainCorpus);
		//aggregateAnnotations(annotatedSentences);
	}
}
