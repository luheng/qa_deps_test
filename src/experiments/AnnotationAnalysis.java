package experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import constraints.AbstractConstraint;
import constraints.AnswerIsHeadlessSubtreeConstraint;
import constraints.AnswerIsSingleSpanConstraint;
import constraints.AnswerIsSubtreeConstraint;
import constraints.EntireAnswerIsSingleSpanConstraint;
import constraints.NonEmptyQuestionConstraint;
import constraints.ReversedEdgeQAConstraint;
import constraints.SingleEdgeQAConstraint;
import annotation.GreedyQuestionAnswerAligner;
import data.AnnotatedSentence;
import data.DepCorpus;
import data.DepSentence;
import data.QAPair;

public class AnnotationAnalysis {
	
	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static String annotationFilename = "manual_annotation/en-train-50sentences.txt";
			
	public static void main(String[] args) {
		DepCorpus trainCorpus = new DepCorpus("en-universal-train");
		
		try {
			trainCorpus.loadCoNLL(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		// Read annotation
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
							trainCorpus.sentences.get(sentID));
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Print annotation
		/*
		for (AnnotatedSentence sentence : annotatedSentences) {
			System.out.println(sentence.toString());
		}
		*/
		
		// Align
		GreedyQuestionAnswerAligner aligner = new GreedyQuestionAnswerAligner();
		AbstractConstraint singleSpanConstraint = new EntireAnswerIsSingleSpanConstraint(),
						   subtreeConstraint = new AnswerIsSubtreeConstraint(),
						   headlessSubtreeConstraint = new AnswerIsHeadlessSubtreeConstraint(),
						   nonEmptyQuestionConstraint = new NonEmptyQuestionConstraint(),
						   singleEdgeQAConstraint = new SingleEdgeQAConstraint(),
						   reversedEdgeQAConstraint = new ReversedEdgeQAConstraint();
		
		int qaCounter = 0, totalQAPairs = 0;
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence depSentence = sentence.depSentence;
			String[] tokens = trainCorpus.wordDict.getStringArray(depSentence.tokens);
			
			for (QAPair qa : sentence.qaList) {
				aligner.align(sentence.depSentence, qa);
				
				boolean isSingleSpan = singleSpanConstraint.validate(depSentence, qa),
						isSubtree = subtreeConstraint.validate(depSentence, qa),
						isHeadlessSubtree = headlessSubtreeConstraint.validate(depSentence, qa),
						isNonEmptyQuestion = nonEmptyQuestionConstraint.validate(depSentence, qa),
						isSingleEdgeQA = singleEdgeQAConstraint.validate(depSentence, qa),
						isReversedEdgeQA = reversedEdgeQAConstraint.validate(depSentence, qa);
					
				// if (!isSingleSpan) {
				// if (!isSubtree) {
				// if (!isSubtree && !isHeadlessSubtree) {
				// if (isSubtree || isHeadlessSubtree) {
				// if (!isSingleEdgeQA) {
				// if (!isSingleEdgeQA && !isReversedEdgeQA) {
				if (sentence.depSentence.length < 15) {
					// print sentence
					System.out.println(sentence.toString());
					for (int i = 0; i < sentence.depSentence.length; i++) {
						System.out.print(String.format("%d %s (%d)\t", i,
								tokens[i], sentence.depSentence.parents[i]));
					}
					System.out.println();
					
					// print alignment
					qa.printAlignment();	
					System.out.println(singleSpanConstraint.toString() + "\t" + isSingleSpan);
					System.out.println(subtreeConstraint.toString() + "\t" + isSubtree);
					System.out.println(headlessSubtreeConstraint.toString() + "\t" + isHeadlessSubtree);
					System.out.println(nonEmptyQuestionConstraint.toString() + "\t" + isNonEmptyQuestion);
					System.out.println(singleEdgeQAConstraint.toString() + "\t" + isSingleEdgeQA);
					System.out.println(reversedEdgeQAConstraint.toString() + "\t" + isReversedEdgeQA);
					System.out.println();		
					qaCounter += 1;
				}
				totalQAPairs += 1;
			}
		}
		System.out.println("Total number of qa pairs:\t" + totalQAPairs);
		System.out.println("Number of answers that are not a single subtree:\t" + qaCounter);
	}
}
