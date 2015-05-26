package baselines;

import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.SRLCorpus;

public class IAAOutputExperiment {

	private static String dataPath =
		"/Users/luheng/versioned/qa_deps_test/odesk_agreement/annotated";
	
	private static String[] inputFiles1 = {
		"odesk_pb_r3_s30_breanna.xlsx",
		"odesk_pb_r3_s30_ellen.xlsx",
		"odesk_pb_r3_s30_john.xlsx",
		"odesk_pb_r3_s30_katie.xlsx",
		"odesk_pb_r3_s30_tracy.xlsx"
	};
	
	private static String[] inputFiles2 = {
		"odesk_wiki1_r014_s30_breanna.xlsx",
		"odesk_wiki1_r014_s30_ellen.xlsx",
		"odesk_wiki1_r014_s30_john.xlsx",
		"odesk_wiki1_r014_s30_katie.xlsx",
		"odesk_wiki1_r014_s30_tracy.xlsx"
	};
	
	private static int numAnnotators = 5;
	
	private static void readData(
			String[] inputFiles,
			String outputPathPrefix,
			Corpus baseCorpus,
			HashMap<Integer, AnnotatedSentence> annotations) {
		try {
			XSSFDataRetriever.readXSSFAnnotation(
					inputFiles,
					baseCorpus,
					annotations);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(String.format("Read %d sentences.",
				annotations.size()));
		if (SRLCorpus.class.isInstance(baseCorpus)) {
			System.out.println(String.format("Validating %d sentences.",
					annotations.size()));
			SRLAnnotationValidator tester = new SRLAnnotationValidator();
			tester.computeSRLAccuracy(annotations.values(),
					(SRLCorpus) baseCorpus);
			tester.ignoreLabels = true;
			tester.computeSRLAccuracy(annotations.values(),
					(SRLCorpus) baseCorpus);
		}
	}
	
	private static String[] getAnnotators(
			HashMap<Integer, AnnotatedSentence> annotations) {
		HashSet<String> annotators = new HashSet<String>();
		for (AnnotatedSentence annot : annotations.values()) {
			for (int propHead : annot.qaLists.keySet()) {
				for (QAPair qa : annot.qaLists.get(propHead)) {
					String annotator = (String) qa.annotators.get(0);
					annotators.add(getShortAnnotatorName(annotator));
				}
			}
		}
		return annotators.toArray(new String[annotators.size()]);
	}
	
	private static String getShortAnnotatorName(String annotator) {
		String[] tmp = annotator.split("_");
		return tmp[tmp.length - 1].split("\\.")[0];
	}
	
	private static void computeAgreement(
			HashMap<Integer, AnnotatedSentence> annotations) {
		String[] annotators = getAnnotators(annotations);
		for (AnnotatedSentence annotSent : annotations.values()) {
			System.out.println(annotSent.sentence.sentenceID + "\t" + annotSent.sentence.getTokensString());
			
			for (int propHead : annotSent.qaLists.keySet()) {
				System.out.println(annotSent.sentence.getTokenString(propHead));

				for (int i = 0; i < annotators.length; i++) {
					System.out.println("A" + i);
					for (QAPair qa : annotSent.qaLists.get(propHead)) {
						String annt = getShortAnnotatorName((String) qa.annotators.get(0));
						if (!annt.equals(annotators[i])) {
							continue;
						}
						System.out.println(" \t" + qa.getQuestionString() + "\t" + qa.getAnswerString());
					}
				}
				
			}
		}
	}
	
	public static void main(String[] args) {
		//SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus("PROPBANK");		
		//processData(kXssfInputFiles, kOutputPathPrefix, srlCorpus, 0.6, 0.2);
		
		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
		String[] files = new String[numAnnotators];
		for (int i = 0; i < numAnnotators; i++) {
			files[i] = dataPath + "/" + inputFiles2[i];
		}
		
		//WikipediaCorpus corpus = new WikipediaCorpus("corpus");
		Corpus corpus = new Corpus("corpus");
		readData(files, "", corpus, annotations);
		
		computeAgreement(annotations);
	}
}
