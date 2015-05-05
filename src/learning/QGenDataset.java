package learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;


public class QGenDataset {

	public String datasetName;
	Corpus corpus;
	public ArrayList<AnnotatedSentence> sentences;
	public ArrayList<QASample> samples;
	public ArrayList<QAPair> questions;
	public HashMap<Integer, Sentence> sentenceMap;
	
	public QGenDataset(Corpus corpus, String name) {
		this(corpus);
		this.datasetName = name;
	}
	
	public QGenDataset(Corpus corpus) {
		this.corpus = corpus;
		this.sentences = new ArrayList<AnnotatedSentence>();
		this.questions = new ArrayList<QAPair>();
		this.samples = new ArrayList<QASample>();
		this.sentenceMap = new HashMap<Integer, Sentence>();
	}
	
	public HashSet<Integer> getSentenceIds() {
		HashSet<Integer> sentIds = new HashSet<Integer>();
		for (QAPair qa : questions) {
			sentIds.add(qa.sentence.sentenceID);
		}
		return sentIds;
	}

	public void loadData(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				new File(filePath)));
		String currLine = "";
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.trim().split("\t");
			assert (info.length == 2);
			currLine = reader.readLine();
			Sentence sent = corpus.addNewSentence(currLine);
			sent.source = info[0];
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
					QAPair qa = new QAPair(sent, propHead, j, question);
					String[] answers = info[8].split("###");
					for (String answer : answers) {
						qa.addAnswer(answer);
					}
					annotSent.addQAPair(propHead, qa);
					questions.add(qa);
				}
			}
			reader.readLine();
			sentences.add(annotSent);
		}
		reader.close();
		System.out.println(String.format("Read %d sentences from %s.",
				sentences.size(), filePath));
		
		for (AnnotatedSentence sent : sentences) {
			sentenceMap.put(sent.sentence.sentenceID, sent.sentence);
		}
	}
		
	public void generateSamples(KBestParseRetriever syntaxHelper) {
		for (AnnotatedSentence annotSent : sentences) {
			for (int propHead : annotSent.qaLists.keySet()) {
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					samples.add(syntaxHelper.generateQGenSample(
							annotSent.sentence, propHead, qa));
				}
			}
		}
		System.out.println(String.format("Generated %d samples.",
				samples.size()));
	}
	
	public void loadSamples(String filePath)
			throws IOException, ClassNotFoundException {
		ObjectInputStream istream =
				new ObjectInputStream(new FileInputStream(filePath));
		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) istream.readObject();
		for (int i = 0; i < objs.size(); i++) {
			QASample sample = (QASample) objs.get(i);
			samples.add(sample);
		
		}
		istream.close();
		System.out.println(String.format("Loaded %d samples from %s.",
				samples.size(), filePath));
	}
}

