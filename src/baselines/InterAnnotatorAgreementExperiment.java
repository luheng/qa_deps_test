package baselines;

import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.SRLCorpus;
import data.WikipediaCorpus;

public class InterAnnotatorAgreementExperiment {

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
			/*
			XSSFDataRetriever.outputAnnotations(
					outputPathPrefix + ".qa",
					baseCorpus,
					annotations);
			*/
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
	
	
	public static void main(String[] args) {
		//SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus("PROPBANK");		
		//processData(kXssfInputFiles, kOutputPathPrefix, srlCorpus, 0.6, 0.2);
		
		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
		String[] files = new String[inputFiles1.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = dataPath + "/" + inputFiles1[i];
		}
		WikipediaCorpus corpus = new WikipediaCorpus("corpus");
		readData(files, "", corpus, annotations);
		
		for (AnnotatedSentence annotSent : annotations.values()) {
			System.out.println(annotSent.sentence.getTokensString());
			for (int propHead : annotSent.qaLists.keySet()) {
				System.out.println(annotSent.sentence.getTokenString(propHead));
				// TODO: For each annotator, convert question to qlabel
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String qlabel = QuestionEncoder.getQuestionLabel(qa.questionWords);
					String[] tmp = qa.annotator.split("_");
					String annotName = tmp[tmp.length - 1].split("\\.")[0];
					System.out.println(annotName + "\t" + qlabel + "\t" + qa.getQuestionString() + "\t" + qa.getAnswerString());
				}
			}
		}
	}
	
}
