package baselines;

import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import annotation.SRLAnnotationValidator;
import annotation.SRLCoverageValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.SRLCorpus;
import experiments.ExperimentUtils;

public class PropBankCoverageExperiment {

	private static String[] kXssfInputFiles = {
				"odesk/raw_annotation/odesk_r2_s90_donna_fixed.xlsx",
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
	
	private static void processData(String[] inputFiles, Corpus baseCorpus) {
		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
		try {
			XSSFDataRetriever.readXSSFAnnotation(
					inputFiles,
					baseCorpus,
					annotations);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(String.format("Validating %d sentences.",
				annotations.size()));
		// Compute coverage
		
		SRLCoverageValidator tester = new SRLCoverageValidator();
		tester.computeCoverage(annotations.values(), (SRLCorpus) baseCorpus);
	}
	
	
	public static void main(String[] args) {
		SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus("PROPBANK");		
		processData(kXssfInputFiles, srlCorpus);
	}
}
