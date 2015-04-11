package experiments;

import java.io.IOException;
import java.util.HashMap;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.SRLCorpus;

public class BaselineQAExperiment {

	
	private static String[] xssfInputFiles = {
			"odesk/raw_annotation/odesk_r2_s90_breanna_fixed.xlsx",
			"odesk/raw_annotation/odesk_r2_s90_donna_fixed.xlsx",
			"odesk/raw_annotation/odesk_r3_s100_b02_katie.xlsx",
			"odesk/raw_annotation/odesk_r3_s100_breanna_fixed.xlsx",
			"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx",
			"odesk/raw_annotation/odesk_r7_s100_ellen_fixed.xlsx"
	};
	

	private static void aggregateAnnotations(
			SRLCorpus corpus,
			HashMap<Integer, HashMap<String, AnnotatedSentence>> annotations) {
		System.out.println(String.format("Processing %d sentences.", annotations.size()));
		for (int sid : annotations.keySet()) {
			
		}
	}
	
	public static void main(String[] args) {
		SRLCorpus baseCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		
		HashMap<Integer, HashMap<String, AnnotatedSentence>> annotations =
				new HashMap<Integer, HashMap<String, AnnotatedSentence>>();
		
		XSSFDataRetriever.readXSSFAnnotation(
				xssfInputFiles,
				baseCorpus,
				annotations);
		
		aggregateAnnotations(baseCorpus, annotations);

		/*
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotations.values(), baseCorpus);
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences.values(), baseCorpus);
		*/
		//	debugOutput(trainCorpus, annotatedSentences);
	}
}
