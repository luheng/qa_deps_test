package experiments;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;

public class BaselineQADataProcessor {

	private static String[] xssfInputFiles = {
		//		"odesk/raw_annotation/odesk_r2_s90_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r2_s90_donna_fixed.xlsx",
		//		"odesk/raw_annotation/odesk_r3_s100_b02_katie.xlsx",
		//		"odesk/raw_annotation/odesk_r3_s100_b02_john.xlsx",
				"odesk/raw_annotation/odesk_r3_s100_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r6_p1_s50_francine_fixed.xlsx",
				"odesk/raw_annotation/odesk_r7_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r8_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r10_p1_s50_john_fixed.xlsx"
		};
		
	private static String outputPathPrefix = "data/odesk_s600";
		
	private static String qaInputPath = "data/odesk_s600.qa";
	
	private static final int randomSeed = 12345;
	
	private static void processData(String[] inputPaths,
			String outputPathPrefix,
			double splitRatio) {
		
		SRLCorpus baseCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "PROPBANK");

		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
		
		try {
			XSSFDataRetriever.readXSSFAnnotation(
					xssfInputFiles,
					baseCorpus,
					annotations);
			XSSFDataRetriever.outputAnnotations(
					outputPathPrefix + ".qa",
					baseCorpus,
					annotations);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ************* Split train and test ************ 
		ArrayList<Integer> sentIds = new ArrayList<Integer>();
		sentIds.addAll(annotations.keySet());
		Collections.shuffle(sentIds, new Random(randomSeed));
		int numTrains = (int) (sentIds.size() * splitRatio);
		HashMap<Integer, AnnotatedSentence>
				trainSents = new HashMap<Integer, AnnotatedSentence>(),
				testSents = new HashMap<Integer, AnnotatedSentence>();
		for (int i = 0; i < sentIds.size(); i++) {
			int sentId = sentIds.get(i);
			if (i < numTrains) {
				trainSents.put(sentId, annotations.get(sentId));
			} else {
				testSents.put(sentId, annotations.get(sentId));
			}
		}
		
		try {
			XSSFDataRetriever.outputAnnotations(
					outputPathPrefix + ".train.qa",
					baseCorpus,
					trainSents);
			XSSFDataRetriever.outputAnnotations(
					outputPathPrefix + ".test.qa",
					baseCorpus,
					testSents);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		System.out.println(String.format("Validating %d sentences.",
				annotations.size()));
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotations.values(), baseCorpus);
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotations.values(), baseCorpus);
	}
	
	public static void main(String[] args) {
		processData(xssfInputFiles, outputPathPrefix, 0.6);
		//	debugOutput(trainCorpus, annotatedSentences);
	}
}
