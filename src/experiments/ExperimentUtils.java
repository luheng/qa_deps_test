package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import config.ExperimentDataConfig;
import util.CSVUtils;
import util.StringUtils;
import annotation.DistanceSensitiveQuestionAnswerAligner;
import annotation.GreedyQuestionAnswerAligner;
import annotation.AbstractQuestionAnswerAligner;
import data.AnnotatedDepSentence;
import data.Corpus;
import data.DepCorpus;
import data.QAPairOld;
import data.SRLCorpus;
import data.UniversalPostagMap;
import data.VerbInflectionDictionary;
import data.WikipediaCorpus;

public class ExperimentUtils {
	public static boolean useNumberedAnnotation = false;
	public static boolean useDistanceSensitiveAlignment = true;
	
	public static int maxNumSentences = 18;
	
	public static DepCorpus loadDepCorpus() {
		DepCorpus corpus = new DepCorpus("en-universal-train");
		try {
			corpus.loadUniversalDependencyData(ExperimentDataConfig.get("depTrainFilename"));
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	public static SRLCorpus loadSRLCorpus(String corpusName) {
		SRLCorpus corpus = new SRLCorpus(corpusName);
		UniversalPostagMap univmap = new UniversalPostagMap();
		try {
			univmap.loadFromFile(ExperimentDataConfig.get("enUnivPostagFilename"));
			corpus.loadCoNLL2009Data(ExperimentDataConfig.get("srlTrainFilename"),
									 univmap,
									 true /* load gold syntax info */);	
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	public static SRLCorpus loadSRLCorpus(String corpusPath,
			String corpusName) {
		SRLCorpus corpus = new SRLCorpus(corpusName);
		UniversalPostagMap univmap = new UniversalPostagMap();
		try {
			univmap.loadFromFile(ExperimentDataConfig.get("enUnivPostagFilename"));
			corpus.loadCoNLL2009Data(corpusPath,
									 univmap,
									 true /* load gold syntax info */);	
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	public static WikipediaCorpus loadWikiCorpus(String corpusPath,
			String corpusName) {
		WikipediaCorpus corpus = new WikipediaCorpus(corpusName);
		try {
			corpus.loadFromWikiMetaFile(corpusPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	public static UniversalPostagMap loadPostagMap() {
		UniversalPostagMap univmap = new UniversalPostagMap();
		try {
			univmap.loadFromFile(ExperimentDataConfig.get("enUnivPostagFilename"));
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return univmap;
	}
	
	public static VerbInflectionDictionary loadInflectionDictionary(Corpus corpus) {
		VerbInflectionDictionary inflDict = new VerbInflectionDictionary(corpus);
		try {
			inflDict.loadDictionaryFromFile(
					ExperimentDataConfig.get("wiktionaryVerbsFilename"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return inflDict;
	}
		
	public static ArrayList<AnnotatedDepSentence> loadAnnotatedSentences(
			DepCorpus corpus) {
		BufferedReader reader;
		ArrayList<AnnotatedDepSentence> annotatedSentences =
				new ArrayList<AnnotatedDepSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(ExperimentDataConfig.get(
							"trailAnnotationFilename"))));
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
					AnnotatedDepSentence sentence = new AnnotatedDepSentence(
							corpus.getSentence(sentID));
					annotatedSentences.add(sentence);
				} else {
					String[] info = line.split("###");
					if (info.length < 2) {
						System.out.println("Error parsing line: " + line);
						continue;
					}
					annotatedSentences.get(sentPtr).addQA(
							new QAPairOld(info[0].trim(), info[1].trim()));
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
	public static ArrayList<AnnotatedDepSentence> loadNumberedAnnotation(
			DepCorpus corpus) {
		BufferedReader reader;
		ArrayList<AnnotatedDepSentence> annotatedSentences =
				new ArrayList<AnnotatedDepSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(ExperimentDataConfig.get(
							"trailAnnotationFilename"))));
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
					AnnotatedDepSentence sentence = new AnnotatedDepSentence(
							corpus.getSentence(sentID));
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
							QAPairOld.parseNumberedQAPair(question, answer));
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
	@Deprecated
	public static ArrayList<AnnotatedDepSentence> loadSRLAnnotationSentences(
			SRLCorpus corpus) {
		
		BufferedReader reader;
		ArrayList<AnnotatedDepSentence> annotatedSentences =
				new ArrayList<AnnotatedDepSentence>();
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(ExperimentDataConfig.get("trialAnnotationFilename"))));
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
						AnnotatedDepSentence sentence = new AnnotatedDepSentence(
							corpus.getSentence(sentID));
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
					AnnotatedDepSentence sentence =
							annotatedSentences.get(annotatedSentences.size() - 1);
					
					if (!info[6].isEmpty()) {
						String extendedAnswer = info[6].trim() + " " +
								answerString;
						
						//if (sentence.depSentence.getTokensString()
						//		.contains(extendedAnswer)) {
						if (sentence.depSentence.getTokensString()
								.contains(info[6].trim())) {
							answerString = extendedAnswer;
						}
					}
					
					annotatedSentences.get(annotatedSentences.size() - 1)
						.addQA(new QAPairOld(questionString, answerString,
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
		for (AnnotatedDepSentence sentence : annotatedSentences) {
			for (QAPairOld qa : sentence.qaList) {
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
			(ArrayList<AnnotatedDepSentence> annotatedSentences) {
		AbstractQuestionAnswerAligner aligner;
		if (useDistanceSensitiveAlignment) {
			aligner = new DistanceSensitiveQuestionAnswerAligner();
		} else {
			aligner = new GreedyQuestionAnswerAligner();
		}
		for (AnnotatedDepSentence sentence : annotatedSentences) {
			for (QAPairOld qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
	}
	
}