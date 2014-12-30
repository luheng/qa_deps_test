package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import util.CSVUtils;
import util.StringUtils;
import annotation.DistanceSensitiveQuestionAnswerAligner;
import annotation.GreedyQuestionAnswerAligner;
import annotation.AbstractQuestionAnswerAligner;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.QAPair;
import data.SRLCorpus;
import data.UniversalPostagMap;

public class ExperimentUtils {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static final String srlTrainFilename =
			"/Users/luheng/data/conll05st-release/conll05_train.srl";
	
	public static final String srlAnnotationFilename =
			//"manual_annotation/sr_pilot_annotation_mike.txt";
			"manual_annotation/sr_pilot_annotation_luheng.txt";
			//"manual_annotation/sr_pilot_annotation_luke.txt";
	
	public static final String conll2009TrialFilename =
			//"/Users/luheng/data/CoNLL-2009/CoNLL2009-ST-English-trial.txt";
			"/Users/luheng/data/CoNLL2009-ST-English/CoNLL2009-ST-English-trial.txt";
	
	public static final String enUnivPostagFilename =
			"/Users/luheng/data/CONLL-x/univmap/en-ptb.map";
	
	public static String annotationFilename = "manual_annotation/en-train-50sentences.txt";
	// public static String annotationFilename = "manual_annotation/en-upperbound.txt";
	// public static String annotationFilename = "manual_annotation/luke_first5.csv";
	public static boolean useNumberedAnnotation = false;
	public static boolean useDistanceSensitiveAlignment = true;
	
	public static int maxNumSentences = 10;
	
	public static DepCorpus loadDepCorpus() {
		DepCorpus corpus = new DepCorpus("en-universal-train");
		try {
			corpus.loadUniversalDependencyData(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	public static SRLCorpus loadSRLCorpus() {
		SRLCorpus corpus = new SRLCorpus("en-srl-trial");
		UniversalPostagMap univmap = new UniversalPostagMap();
		try {
			univmap.loadFromFile(enUnivPostagFilename);
			corpus.loadCoNLL2009Data(ExperimentUtils.conll2009TrialFilename,
									 univmap,
									 true /* load gold syntax info */);	
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
		
	public static ArrayList<AnnotatedSentence> loadAnnotatedSentences(
			DepCorpus corpus) {
		BufferedReader reader;
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(annotationFilename)));
			String line;
			int sentPtr = 0;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					if (annotatedSentences.size() > sentPtr) {
						// Expecting a new sentence
						sentPtr += 1;
						if (sentPtr >= maxNumSentences) {
							break;
						}
					}
				} else if (annotatedSentences.size() <= sentPtr) {
					String[] info = line.split("\t");
					int sentID = Integer.parseInt(info[0]);
					AnnotatedSentence sentence = new AnnotatedSentence(
							corpus.sentences.get(sentID));
					annotatedSentences.add(sentence);
				} else {
					String[] info = line.split("###");
					if (info.length < 2) {
						System.out.println("Error parsing line: " + line);
						continue;
					}
					annotatedSentences.get(sentPtr).addQA(
							new QAPair(info[0].trim(), info[1].trim()));
				}
			}
			System.out.println(String.format("Read %d annotated sentences.",
					annotatedSentences.size()));
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return annotatedSentences;
	}
	
	/* Read annotation in CSV format.
	 *
	 */
	public static ArrayList<AnnotatedSentence> loadNumberedAnnotation(
			DepCorpus corpus) {
		BufferedReader reader;
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(annotationFilename)));
			String line, lastQuestion = "";
			int sentPtr = 0;
			while ((line = reader.readLine()) != null) {
				ArrayList<String> columns = CSVUtils.getColumns(line.trim());
				if (StringUtils.isEmptyStringArray(columns)) {
					// Expecting a new sentence
					sentPtr += 1;
					if (sentPtr >= maxNumSentences) {
						break;
					}
				} else if (annotatedSentences.size() <= sentPtr) {
					// Getting sentence info.
					int sentID = Integer.parseInt(columns.get(0));
					AnnotatedSentence sentence = new AnnotatedSentence(
							corpus.sentences.get(sentID));
					annotatedSentences.add(sentence);
				} else {
					// Getting numbered QA pair.
					// If question is empty use previous one.
					String question = columns.get(1),
						   answer = columns.get(2);
					if (question.trim().isEmpty()) {
						question = lastQuestion;
					} else {
						lastQuestion = question;
					}
					annotatedSentences.get(sentPtr).addQA(
							QAPair.parseNumberedQAPair(question, answer));
				}
			}
			System.out.println(String.format("Read %d annotated sentences.",
					annotatedSentences.size()));
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return annotatedSentences;
	}
	
	/**
	 * Load SRL annotated sentence (Tab delimited)
	 * The format is specific to the pilot annotation.
	 * @param corpus
	 * @return
	 */
	public static ArrayList<AnnotatedSentence> loadSRLAnnotationSentences(
			SRLCorpus corpus) {
		
		BufferedReader reader;
		ArrayList<AnnotatedSentence> annotatedSentences =
				new ArrayList<AnnotatedSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(srlAnnotationFilename)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				}
				String[] info = line.split("\t");
				if (info.length < 10) {
					try {
						int sentID = Integer.parseInt(info[0]) - 1;
						if (sentID >= maxNumSentences) {
							break;
						}
						AnnotatedSentence sentence = new AnnotatedSentence(
							corpus.sentences.get(sentID));
						annotatedSentences.add(sentence);
					} catch (NumberFormatException e) {
						System.out.println("Error parsing line: " + line);
						continue;
					}
				} else {
					if (info[0].equals("[Propositions]")) {
						continue;
					}
					// Slot 0: Proposition
					// Slot 1: Wh-word
					// Slot 6: Preposition
					// Slot 9: Answer
					String propositionString = info[0];
					String questionString = StringUtils.join(" ", info, 1, 9);
					String answerString = info[9].trim();
				
					// Special case: if the question contains a preposition, and
					// that same preposition immediately precedes the answer
					// span, we attach it to the beginning of the answer.
					AnnotatedSentence sentence =
							annotatedSentences.get(annotatedSentences.size() - 1);
					
					if (!info[6].isEmpty()) {
						String extendedAnswer = info[6].trim() + " " +
								answerString;
						if (sentence.depSentence.getTokensString()
								.contains(extendedAnswer)) {
							answerString = extendedAnswer;
						}
					}
					
					annotatedSentences.get(annotatedSentences.size() - 1)
						.addQA(new QAPair(questionString, answerString,
								propositionString));		
				}
			}
			System.out.println(String.format("Read %d annotated sentences.",
					annotatedSentences.size()));
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Align SRL QAs and Propositions.
		AbstractQuestionAnswerAligner aligner;
		aligner = new DistanceSensitiveQuestionAnswerAligner();
		for (AnnotatedSentence sentence : annotatedSentences) {
			for (QAPair qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
		
		// Debug: check alignment.
		/*
		for (AnnotatedSentence sentence : annotatedSentences) {
			sentence.prettyPrintAlignment();
		}
		*/
		return annotatedSentences;
	}
	
	public static void doGreedyAlignment
			(ArrayList<AnnotatedSentence> annotatedSentences) {
		AbstractQuestionAnswerAligner aligner;
		if (useDistanceSensitiveAlignment) {
			aligner = new DistanceSensitiveQuestionAnswerAligner();
		} else {
			aligner = new GreedyQuestionAnswerAligner();
		}
		for (AnnotatedSentence sentence : annotatedSentences) {
			for (QAPair qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
	}
	
}