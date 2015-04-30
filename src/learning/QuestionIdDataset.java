package learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;

import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Feature;

public class QuestionIdDataset {

	public String datasetName;
	Corpus corpus;
	CountDictionary questionLabelDict;
	ArrayList<AnnotatedSentence> sentences;
	ArrayList<QAPair> questions;
	ArrayList<QASample> samples;
	Feature[][] features;
	double[] labels;
	
	public QuestionIdDataset(Corpus corpus, CountDictionary qdict, String name) {
		this(corpus, qdict);
		this.datasetName = name;
	}
	
	public QuestionIdDataset(Corpus corpus, CountDictionary qdict) {
		this.corpus = corpus;
		this.questionLabelDict = qdict;
		this.sentences = new ArrayList<AnnotatedSentence>();
		this.questions = new ArrayList<QAPair>();
	}
	
	public Corpus getCorpus() {
		return corpus;
	}
	 
	public ArrayList<QASample> getSamples() {
		return samples;
	}
	
	public ArrayList<QAPair> getQuestions() {
		return questions;
	}

	public Feature[][] getFeatures() {
		return features;
	}
	
	public double[] getLabels() {
		return labels;
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
	}
	
	public void generateSamples(KBestParseRetriever syntaxHelper) {
		// For each <sentence, target> pair, generate a set of samples
		
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
	
	public void extractFeaturesAndLabels(
			AnswerIdFeatureExtractor featureExtractor) {
		int numSamples = samples.size(),
			numQuestions = questions.size();
		features = new Feature[numSamples][];
		labels = new double[numSamples];
		
	}

}

