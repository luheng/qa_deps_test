package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import optimization.DualDecompositionOptimizer;
import util.LatticeUtils;
import util.StringUtils;
import annotation.GreedyQuestionAnswerAligner;
import data.Accuracy;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.DepSentence;
import data.Evaluation;
import data.QAPair;
import decoding.AdjacencyGraph;
import decoding.Decoder;
import decoding.QADecoder;
import decoding.ViterbiDecoder;

public class DualDecompositionExperiment {
	
	// TODO: move stuff to config.
	public static final String trainFilename =
				"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
				"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
				"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
		
	public static String annotationFilename = "manual_annotation/en-train-50sentences.txt";
	
	private static DepCorpus loadDepCorpus() {
		DepCorpus corpus = new DepCorpus("en-universal-train");
		try {
			corpus.loadCoNLL(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	private static ArrayList<AnnotatedSentence> loadAnnotatedSentences(
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
	
	public static void main(String[] args) {
		DepCorpus trainCorpus = loadDepCorpus();
		ArrayList<AnnotatedSentence> annotatedSentences =
				loadAnnotatedSentences(trainCorpus);
		
		// Do alignment.
		GreedyQuestionAnswerAligner aligner = new GreedyQuestionAnswerAligner();
		for (AnnotatedSentence sentence : annotatedSentences) {
			for (QAPair qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
		
		// Do dual decomposition.
		Decoder viterbiDecoder = new ViterbiDecoder();
		QADecoder qaDecoder = new QADecoder();
		DualDecompositionOptimizer optimizer = new DualDecompositionOptimizer();
		optimizer.run(annotatedSentences, viterbiDecoder, qaDecoder, 100, 0.1);
		
	}
		
}
