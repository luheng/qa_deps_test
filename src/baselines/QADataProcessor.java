package baselines;

import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.SRLCorpus;
import experiments.ExperimentUtils;

public class QADataProcessor {

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

	private static String[] kAgreementInputFiles = {
		//	"odesk_agreement/camera_ready/annotated/odesk_r3_s70_breanna.xlsx",
		//	"odesk_agreement/camera_ready/annotated/odesk_r3_s70_donna.xlsx",
		//	"odesk_agreement/camera_ready/annotated/odesk_r3_s70_john.xlsx",
		//	"odesk_agreement/camera_ready/annotated/odesk_r3_s70_katie.xlsx",
		//	"odesk_agreement/camera_ready/annotated/odesk_r3_s70_tracy.xlsx",
	};
	
	private static String[] kWikifInputFiles = {
		"odesk_wiki/raw_annotation/odesk_wiki1_r000_katie_new.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r001_katie.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r002_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r003_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r004_sarah.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r005_sarah.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r006_tracy.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r007_tracy.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r008_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r009_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r010_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r011_maria.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r012_maria.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r013_john.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r014_john.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r015_francine.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r016_francine.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r017_donna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r018_donna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r019_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r020_breanna.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r021_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r022_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r023_tracy.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r024_tracy.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r025_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r026_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r027_john.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r028_sarah.xlsx",
		// 29, 30 - breanna
		"odesk_wiki/raw_annotation/odesk_wiki1_r031_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r032_ellen.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r033_katie.xlsx",
		"odesk_wiki/raw_annotation/odesk_wiki1_r034_katie.xlsx",
	};

	private static String kOutputPathPrefix = "data/propbank_new";
	private static String kWikiOutputPathPrefix = "data/wiki1";
		
	private static final int randomSeed = 12345;
	
	private static void processData(String[] inputFiles,
			String outputPathPrefix,
			Corpus baseCorpus,
			double trainRatio, double devRatio) {
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
		
		int numTrains = (int) (sentIds.size() * trainRatio);
		int numDevs = (int) (sentIds.size() * devRatio);
		HashMap<Integer, AnnotatedSentence>
				trainSents = new HashMap<Integer, AnnotatedSentence>(),
				devSents =  new HashMap<Integer, AnnotatedSentence>(),
				testSents = new HashMap<Integer, AnnotatedSentence>();
		
		for (int i = 0; i < sentIds.size(); i++) {
			int sentId = sentIds.get(i);
			if (i < numTrains) {
				trainSents.put(sentId, annotations.get(sentId));
			} else if (i < numTrains + numDevs) {
				devSents.put(sentId, annotations.get(sentId));
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
					outputPathPrefix + ".dev.qa",
					baseCorpus,
					devSents);
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
			System.out.println(String.format("Validating %d sentences.", annotations.size()));
			SRLAnnotationValidator tester = new SRLAnnotationValidator();
			tester.computeSRLAccuracy(annotations.values(), (SRLCorpus) baseCorpus);
			tester.ignoreLabels = true;
			tester.computeSRLAccuracy(annotations.values(), (SRLCorpus) baseCorpus);
		}
	}
	
	
	public static void main(String[] args) {
		SRLCorpus srlCorpus = ExperimentUtils.loadSRLCorpus("PROPBANK");		
		//processData(kXssfInputFiles, kOutputPathPrefix, srlCorpus, 0.6, 0.2);
		//processData(kXssfInputFiles, "", srlCorpus, 0.6, 0.2);

		processData(kAgreementInputFiles, "", srlCorpus, 0.6, 0.2);
		
		//WikipediaCorpus wikiCorpus = new WikipediaCorpus("WIKI1");
		//processData(kWikifInputFiles, kWikiOutputPathPrefix, wikiCorpus, 0.6, 0.2);
	}
}
