package learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Feature;

public class QADataset {
	public String datasetName;
	public Corpus corpus;
	public ArrayList<AnnotatedSentence> sentences;
	public ArrayList<QAPair> questions;
	public ArrayList<QASample> samples;
	public Feature[][] features;
	public double[] labels;
	public HashMap<Integer, Sentence> sentenceMap;
	
	protected QADataset(Corpus corpus, String name) {
		this(corpus);
		this.datasetName = name;
	}
	
	protected QADataset(Corpus corpus) {
		this.corpus = corpus;
		this.sentences = new ArrayList<AnnotatedSentence>();
		this.questions = new ArrayList<QAPair>();
		this.samples = new ArrayList<QASample>();
	}
	
	public Sentence getSentence(int sentId) {
		return sentenceMap.get(sentId);
	}

	public Collection<Integer> getSentenceIds() {
		return sentenceMap.keySet();
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
		for (AnnotatedSentence sent : sentences) {
			sentenceMap.put(sent.sentence.sentenceID, sent.sentence);
		}
		System.out.println(String.format("Read %d sentences from %s.",
				sentences.size(), filePath));
	}
	

	public void loadSamples(String filePath)
			throws IOException, ClassNotFoundException {
		ObjectInputStream istream =
				new ObjectInputStream(new FileInputStream(filePath));
		int numPositiveSamples = 0;
		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) istream.readObject();
		for (int i = 0; i < objs.size(); i++) {
			QASample sample = (QASample) objs.get(i);
			samples.add(sample);
			numPositiveSamples += (sample.isPositiveSample ? 1 : 0);
		}
		istream.close();
		System.out.println(String.format(
				"Loaded %d samples from %s. %d positive, %d negative.",
				samples.size(), filePath, numPositiveSamples,
				samples.size() - numPositiveSamples));
	}
	
}
