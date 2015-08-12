package baselines;

import io.XSSFDataRetriever;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;
import experiments.ExperimentUtils;

public class SideBySidePrinter {

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
	
	private static String outputFilePath = "newswire_annotation_output_2.tsv";

	private static boolean worthOutput(AnnotatedSentence sent) {
		final int minArgPropDist = 5;
		boolean hasNonPBArgs = false, hasLongDistArg = false;
		for (int propId : sent.qaLists.keySet()) {
			ArrayList<QAPair> qaList = sent.qaLists.get(propId);
			SRLSentence sentence = (SRLSentence) sent.sentence;
			String[][] arcs = sentence.getSemanticArcs();
			int numPBArgs = 0;
			for (String a : arcs[propId + 1]) {
				if (!a.isEmpty()) {
					++ numPBArgs;
				}
			}
			if (numPBArgs < qaList.size()) {
				hasNonPBArgs = true;
			}
			for (QAPair qa : qaList) {
				int minDist = sentence.length;
				for (int i = 0; i < qa.answerFlags.length; i++) {
					if (qa.answerFlags[i] > 0 && Math.abs(i - propId) < minDist) {
						minDist = Math.abs(i - propId);
					}
				}
				if (minDist > minArgPropDist) {
					hasLongDistArg = true;
				}
			}
		}
		return hasNonPBArgs && hasLongDistArg;
	}

	private static void processData(String[] inputFiles, Corpus baseCorpus) 
			throws IOException {
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

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				new File(outputFilePath)));
		for (AnnotatedSentence sent : annotations.values()) {
			SRLSentence sentence = (SRLSentence) sent.sentence;

			if (!worthOutput(sent)) {
				continue;
			}

			writer.write(sentence.toString());
			writer.write("--------\n");
			// Output annotations
			ArrayList<Integer> propIds = new ArrayList<>();
			for (int propId : sent.qaLists.keySet()) {
				propIds.add(propId);
			}
			Collections.sort(propIds);
			for (int propId : propIds) {
				writer.write(sentence.getTokenString(propId) + "\n");
				int qaCnt = 0;
				for (QAPair qa : sent.qaLists.get(propId)) {
					writer.write(String.format("QA%d\t%s\t%s\n",
						qaCnt, qa.getQuestionString(), qa.getAnswerString()));
					qaCnt ++;
				}
			}
			writer.write("\n");
		}
		writer.close();
		System.out.println(String.format("Printing %d sentences to %s.",
				annotations.size(), outputFilePath));
	}
	
	
	public static void main(String[] args) {
		SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus("PROPBANK");		
		try {
			processData(kXssfInputFiles, srlCorpus);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
