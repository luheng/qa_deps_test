package baselines;

import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.SRLCorpus;
import data.WikipediaCorpus;
import util.StrUtils;

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
	
	private static int numAnnotators = 5;
	
	private static boolean coreArgsOnly = true;
	
	
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
	
	/*
	private static String[] getAnnotators(
			HashMap<Integer, AnnotatedSentence> annotations) {
		HashSet<String> annotators = new HashSet<String>();
		for (AnnotatedSentence annot : annotations.values()) {
			for (int propHead : annot.qaLists.keySet()) {
				for (QAPair qa : annot.qaLists.get(propHead)) {
					annotators.add(getShortAnnotatorName(qa.annotator));
				}
			}
		}
		return annotators.toArray(new String[annotators.size()]);
	}
	*/
	
	private static String getShortAnnotatorName(String annotator) {
		String[] tmp = annotator.split("_");
		return tmp[tmp.length - 1].split("\\.")[0];
	}
	
	private static boolean equals(QAPair qa1, QAPair qa2) {
		String l1 = qa1.questionLabel.split("=")[0],
			   l2 = qa2.questionLabel.split("=")[0];
		int answerOverlap = 0;
		for (int i = 0; i < qa1.answerFlags.length; i++) {
			if (qa1.answerFlags[i] > 0 && qa2.answerFlags[i] > 0) {
				answerOverlap ++;
			}
		}
		boolean qwEq = l1.split("_")[0].equals(l2.split("_")[0]);
		return qwEq && answerOverlap > 0;
	}
	
	private static void computeAgreement(
			HashMap<Integer, AnnotatedSentence> annotations) {
		double[] microCount = new double[numAnnotators]; // 5 annotators ...
		double[] macroCount = new double[numAnnotators];
		Arrays.fill(microCount, 0.0);
		Arrays.fill(macroCount, 0.0);
		int totalQAs = 0, totalProps = 0;
		
		for (AnnotatedSentence annotSent : annotations.values()) {
			System.out.println(annotSent.sentence.getTokensString());
			
			for (int propHead : annotSent.qaLists.keySet()) {
				System.out.println(annotSent.sentence.getTokenString(propHead));
	
				ArrayList<QAPair> agreedQAs = new ArrayList<QAPair>();
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String annotator = getShortAnnotatorName((String) qa.annotators.get(0));
					boolean agreed = false;
					for (QAPair qa2 : agreedQAs) {
						if (equals(qa, qa2)) {
							for (int i = 0; i < qa2.answerFlags.length; i++) {
								qa2.answerFlags[i] =
										Math.min(qa.answerFlags[i], qa2.answerFlags[i]);
								/*
								if (qa.answerFlags[i] > 0) {
									qa2.answerFlags[i] ++;
								}
								*/
							}
							qa2.annotators.add(annotator);
							agreed = true; break;
						}
					}
					if (!agreed) {						
						QAPair qa2 = new QAPair(
								qa.sentence, propHead, qa.questionWords,
			        			"" /* answer */,
								annotator);
						for (int i = 0; i < qa2.answerFlags.length; i++) {
							qa2.answerFlags[i] = Math.min(1, qa.answerFlags[i]);
						}
						agreedQAs.add(qa2);
					}
				}
				int numAgreedQAs = 0;
				double[] cnt = new double[numAnnotators];
				Arrays.fill(cnt, 0);
				for (QAPair qa : agreedQAs) {
					if (coreArgsOnly && qa.questionLabel.endsWith("=.")) {
						continue;
					}
					System.out.println(
							qa.annotators.size() + "\t" +
							qa.questionLabel + "\t" + 
							qa.getQuestionString() + "\t" +
							qa.getAnswerString() + "\t" +
							StrUtils.join(",", qa.annotators));
					
					for (int i = 0; i < numAnnotators; i++) {
						if (qa.annotators.size() > i) {
							++ cnt[i];
						}
					}
					numAgreedQAs ++;
				}
				for (int i = 0; i < cnt.length; i++) {
					microCount[i] += cnt[i];
					cnt[i] /= numAgreedQAs;
					macroCount[i] += cnt[i];
				}
				System.out.println(
						StrUtils.doubleArrayToString("\t", cnt));
				System.out.println();
				totalQAs += numAgreedQAs;
				if (numAgreedQAs > 0) {
					++ totalProps;
				}
			}
		}
		for (int i = 0; i < microCount.length; i++) {
			microCount[i] /= totalQAs;
			macroCount[i] /= totalProps;
		}
		System.out.println("Micro:\t" +
				StrUtils.doubleArrayToString("\t", microCount));
		System.out.println("Macro:\t" +
				StrUtils.doubleArrayToString("\t", macroCount));
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
		
		WikipediaCorpus corpus = new WikipediaCorpus("corpus");
		readData(files, "", corpus, annotations);
		
		computeAgreement(annotations);
	}
}
