package experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.SRLCorpus;
import data.WikipediaCorpus;

public class BaselineQADataProcessor {

	private static String[] kXssfInputFiles = {
		//		"odesk/raw_annotation/odesk_r2_s90_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r2_s90_donna_fixed.xlsx",
		//		"odesk/raw_annotation/odesk_r3_s100_b02_katie.xlsx",
		//		"odesk/raw_annotation/odesk_r3_s100_b02_john.xlsx",
				"odesk/raw_annotation/odesk_r3_s100_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r5_p1_s50_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r5_p2_s50_maria_fixed.xlsx",
				"odesk/raw_annotation/odesk_r6_p1_s50_francine_fixed.xlsx",
				"odesk/raw_annotation/odesk_r6_p2_s50_tracy_fixed.xlsx",
				"odesk/raw_annotation/odesk_r7_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r8_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r9_p2_s50_katie_fixed.xlsx",
				"odesk/raw_annotation/odesk_r10_p1_s50_john_fixed.xlsx",
				"odesk/raw_annotation/odesk_r10_p2_s50_sarah_fixed.xlsx",
				"odesk/raw_annotation/odesk_r11_p1_s50_katie_fixed.xlsx",
				"odesk/raw_annotation/odesk_r11_p2_s50_john_fixed.xlsx",
				"odesk/raw_annotation/odesk_r12_s100_ellen_fixed.xlsx",
				"odesk/raw_annotation/odesk_r14_p1_s50_tracy_fixed.xlsx",
				"odesk/raw_annotation/odesk_r14_p2_s50_breanna_fixed.xlsx",
				"odesk/raw_annotation/odesk_r15_p1_s50_sarah_fixed.xlsx",
				"odesk/raw_annotation/odesk_r15_p2_s50_john_fixed.xlsx",
		};
	
	private static String[] kWikifInputFiles = {
		"odesk_wiki/raw_annotation/odesk_wiki1_r002_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r003_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r004_sarah.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r008_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r009_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r010_ellen.xlsx",
		};
		
	private static String kOutputPathPrefix = "data/odesk_wiki1";
		
	private static final int randomSeed = 12345;
	
	private static void processData(String[] inputFiles,
			String outputPathPrefix,
			Corpus baseCorpus,
			double splitRatio) {
		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
		try {
			XSSFDataRetriever.readXSSFAnnotation(
					inputFiles,
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
		// SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus(
		//		ExperimentUtils.conll2009TrainFilename, "PROPBANK");
		WikipediaCorpus wikiCorpus = new WikipediaCorpus("WIKI1");
		
		//processData(xssfInputFiles, outputPathPrefix, srlCorpus, 0.6);
		processData(kWikifInputFiles, kOutputPathPrefix, wikiCorpus, 0.6);
		
		//	debugOutput(trainCorpus, annotatedSentences);
	}
}
