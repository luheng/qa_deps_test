package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import annotation.GreedyQuestionAnswerAligner;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.QAPair;

public class ExperimentUtils {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	//public static String annotationFilename = "manual_annotation/en-train-50sentences.txt";
	public static String annotationFilename = "manual_annotation/en-upperbound.txt";
	
	public static int maxNumSentences = 5;
	
	public static DepCorpus loadDepCorpus() {
		DepCorpus corpus = new DepCorpus("en-universal-train");
		try {
			corpus.loadCoNLL(trainFilename);
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
	
	// TODO
	/*
	public static ArrayList<AnnotatedSentence> loadNumberedAnnotation(
			DepCorpus corpus) {
		
	}
	*/
	
	
	public static void doGreedyAlignment
			(ArrayList<AnnotatedSentence> annotatedSentences) {
		GreedyQuestionAnswerAligner aligner = new GreedyQuestionAnswerAligner();
		for (AnnotatedSentence sentence : annotatedSentences) {
			for (QAPair qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
	}
}
