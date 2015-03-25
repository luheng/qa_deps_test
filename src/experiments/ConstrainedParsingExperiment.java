package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import util.LatticeUtils;
import util.StringUtils;
import annotation.GreedyQuestionAnswerAligner;
import data.AnnotatedDepSentence;
import data.DepCorpus;
import data.DepSentence;
import data.QAPairOld;
import decoding.AdjacencyGraph;
import decoding.Decoder;
import decoding.QADecoder;
import decoding.ViterbiDecoder;
import evaluation.Accuracy;
import evaluation.Evaluation;

public class ConstrainedParsingExperiment {
	
	// TODO: move stuff to config.
	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	//public static String annotationFilename = "manual_annotation/en-train-50sentences.txt";
	public static String annotationFilename = "manual_annotation/en-upperbound.txt";
	
	private static DepCorpus loadDepCorpus() {
		DepCorpus corpus = new DepCorpus("en-universal-train");
		try {
			corpus.loadUniversalDependencyData(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		return corpus;
	}
	
	private static ArrayList<AnnotatedDepSentence> loadAnnotatedSentences(
			DepCorpus corpus) {
		BufferedReader reader;
		ArrayList<AnnotatedDepSentence> annotatedSentences =
				new ArrayList<AnnotatedDepSentence>();
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
					AnnotatedDepSentence sentence = new AnnotatedDepSentence(
							corpus.sentences.get(sentID));
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
	
	public static void main(String[] args) {
		DepCorpus trainCorpus = loadDepCorpus();
		ArrayList<AnnotatedDepSentence> annotatedSentences =
				loadAnnotatedSentences(trainCorpus);
		
		// Do alignment.
		GreedyQuestionAnswerAligner aligner = new GreedyQuestionAnswerAligner();
		for (AnnotatedDepSentence sentence : annotatedSentences) {
			for (QAPairOld qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
			}
		}
		
		// Do sentence-wise analysis
		Decoder viterbi = new ViterbiDecoder();
		QADecoder qaVoter = new QADecoder();
		Accuracy baseline1 = new Accuracy(0, 0),
				 baseline2 = new Accuracy(0, 0);

		// double[] tune = {0.1, 0.5, 1.0, 2.0, 5.0, 1.0};
		double[] tune = {10.0};
		//double lambda = 0.5;
		double distWeight = 1.0; 
		
		for (AnnotatedDepSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			int length = depSentence.length;
			int[] prediction = new int[length];
			AdjacencyGraph distScores =
					AdjacencyGraph.getDistanceWeightedGraph(depSentence.length);
			viterbi.decode(distScores.edges, prediction);
			Accuracy acc1 = Evaluation.getAccuracy(depSentence, prediction);
			baseline1.add(acc1);

			double[] results = new double[tune.length];
			for (int t = 0; t < tune.length; t++) {
				double lambda = tune[t];
				
				Arrays.fill(prediction, -1);
				double[][] votes = new double[length + 1][length + 1];
				LatticeUtils.fill(votes, 0.0);
				for (QAPairOld qa : sentence.qaList) {
					qaVoter.vote(depSentence, qa, votes);
				}
				for (int i = 0; i <= length; i++) {
					for (int j = 0; j <= length; j++) {
						distScores.edges[i][j] =
								distWeight * distScores.edges[i][j] +
								lambda * votes[i][j];
					}
				}
				viterbi.decode(distScores.edges, prediction);
				Accuracy acc2 = Evaluation.getAccuracy(depSentence, prediction);
				if (t == 0) {
					baseline2.add(acc2);
				}
				results[t] = acc2.accuracy();
			}
			//if (results[0] > acc1.accuracy()) {
			// Output sentence info.
			System.out.println(String.format("ID: %d\t#tokens: %d\t#qa: %d\tdist: %.2f\tedge_vote: %s",
					depSentence.sentenceID,
					depSentence.length,
					sentence.qaList.size(),
					acc1.accuracy(),
					StringUtils.doubleArrayToString("\t", results)));
			// Output gold and parsed dependency.
			System.out.println(StringUtils.join("\t",
					depSentence.corpus.wordDict.getStringArray(depSentence.tokens)));
			System.out.println(StringUtils.intArrayToString("\t", depSentence.parents));
			System.out.println(StringUtils.intArrayToString("\t", prediction));
			System.out.println(sentence.toString());
			
			//}
		}
			// Print out debugging info.
			/*
			if (depSentence.length < 15) {
				System.out.println(sentence.toString());
				distScores.prettyPrint();
				System.out.println("gold:\t" + StringUtils.intArrayToString("\t", depSentence.parents));
				System.out.println("pred:\t" + StringUtils.intArrayToString("\t", prediction));
				System.out.println(depSentence.sentenceID + "\tacc1:\t" + acc1.accuracy());
				System.out.println(depSentence.sentenceID + "\tacc2:\t" + acc2.accuracy());
				System.out.println();
			}
			*/
		
		System.out.println("Dist accuracy:\t" + baseline1.accuracy());
		System.out.println("QA augmented accuracy:\t" + baseline2.accuracy());
	}
}
