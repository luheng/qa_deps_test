package baselines;

import java.io.IOException;
import java.util.ArrayList;

import config.DataConfig;
import annotation.QuestionEncoder;
import learning.QuestionIdDataset;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;


public class QuestionAnalyzer {
	private Corpus baseCorpus; 	
	private QuestionIdDataset trainSet;
	
	public QuestionAnalyzer() throws IOException {
		baseCorpus = new Corpus("qa-exp-corpus");
		trainSet = new QuestionIdDataset(baseCorpus, "prop-train");
		trainSet.loadData(DataConfig.getDataset(trainSet.datasetName));
		
		int numVerbs = 0;
		for (AnnotatedSentence sent : trainSet.sentences) {
			for (int propHead : sent.qaLists.keySet()) {
				ArrayList<QAPair> qaList = sent.qaLists.get(propHead);
				CountDictionary pfxDict = new CountDictionary();
				for (QAPair qa : qaList) {
					String[] qw = qa.questionWords;
					String[] temp = QuestionEncoder.getLabels(qw);
					String pfx = temp[0].split("=")[0];
					pfxDict.addString(pfx);
					/*
					if (!QuestionEncoder.isPassiveVoice(qw) &&
						(qw[0].equals("who")) &&
						(qw[1].equals("have") || qw[1].equals("had") || qw[1].equals("has")) &&
						!qw[2].isEmpty()) {
						System.out.println(sent.sentence.getTokensString());
						System.out.println(sent.sentence.getTokenString(propHead));
						System.out.println(temp[0] + "\t" +
								qa.getQuestionString() + "\t" +
								qa.getAnswerString());
					}*/
				}
				boolean debug = false;
				for (String pfx : pfxDict.getStrings()) {
					if (pfxDict.getCount(pfx) > 1) {
						debug = true;
					}
				}
				if (!debug) {
					continue;
				}
				numVerbs ++;
				System.out.println(sent.sentence.getTokensString());
				System.out.println(sent.sentence.getTokenString(propHead));
				for (QAPair qa : qaList) {
					String[] temp = QuestionEncoder.getLabels(qa.questionWords);
					System.out.println(temp[0] + "\t" +
							qa.getQuestionString() + "\t" +
							qa.getAnswerString());
				}
			}
		}
		System.out.println(numVerbs);
	}
	
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		QuestionAnalyzer exp = null;
		try {
			exp = new QuestionAnalyzer();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		
	}
}
