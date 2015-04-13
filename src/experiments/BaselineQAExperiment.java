package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Model;

public class BaselineQAExperiment {

	private static String trainFilePath = "data/odesk_s600.train.qa";
	private static String testFilePath = "data/odesk_s600.test.qa";
			
	private static void loadData(String filePath, Corpus corpus,
			ArrayList<AnnotatedSentence> annotations) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
		
		String currLine = "";
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.trim().split("\t");
			assert (info.length == 2);
			currLine = reader.readLine();
			String[] tokens = currLine.trim().split("\\s+");
			int[] tokenIds = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				tokenIds[i] = corpus.wordDict.addString(tokens[i]);
			}
			Sentence sent = new Sentence(tokenIds, corpus, corpus.sentences.size());
			sent.source = info[0];
			corpus.sentences.add(sent);
			int numProps = Integer.parseInt(info[1]);
			AnnotatedSentence annotSent = new AnnotatedSentence(sent);
			for (int i = 0; i < numProps; i++) {
				currLine = reader.readLine();
				info = currLine.split("\t");
				int propHead = Integer.parseInt(info[0]);
				int numQA = Integer.parseInt(info[2]);
				annotSent.addProposition(propHead);
				for (int j = 0; j < numQA; j++) {
					currLine = reader.readLine();
					info = currLine.split("\t");
					assert (info.length == 9);
					String[] question = new String[7];
					for (int k = 0; k < 7; k++) {
						question[k] = (info[k].equals("_") ? "" : info[k]);
					}
					QAPair qa = new QAPair(sent, propHead, question, "", null);
					String[] answers = info[8].split("###");
					for (String answer : answers) {
						qa.addAnswer(answer);
					}
					annotSent.addQAPair(propHead, qa);
				}
			}
			reader.readLine();
			annotations.add(annotSent);
		}
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.",
				annotations.size(), filePath));
	}
	
	private static void trainModel(ArrayList<AnnotatedSentence> trains,
			int cvFolds) {
		
	}
	
	public static void main(String[] args) {
		Corpus corpus = new Corpus("qa-text-corpus");
		ArrayList<AnnotatedSentence> trains = new ArrayList<AnnotatedSentence>(),
								     tests = new ArrayList<AnnotatedSentence>();
		try {
			loadData(trainFilePath, corpus, trains);
			loadData(testFilePath, corpus, tests);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		trainModel(trains, 10);
		
	}
}
