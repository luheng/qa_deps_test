package learning;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

public class AnswerIdDataset {
	Corpus corpus;
	ArrayList<AnnotatedSentence> sentences;
	ArrayList<QAPair> questions;
	ArrayList<QASample> samples;
	int[][] answerFlags, answerHeads;
	Feature[][] features;
	double[] labels;
	
	public AnswerIdDataset(Corpus corpus) {
		this.corpus = corpus;
		this.sentences = new ArrayList<AnnotatedSentence>();
		this.questions = new ArrayList<QAPair>();
		this.samples = new ArrayList<QASample>();
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
	
	public int[][] getAnswerFlags() {
		return answerFlags;
	}
	
	public int[][] getAnswerHeads() {
		return answerHeads;
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
		for (int i = 0; i < questions.size(); i++) {
			ArrayList<QASample> newSamples =
					syntaxHelper.generateSamplesGivenQuestion(questions.get(i));
			for (QASample sample : newSamples) {
				sample.questionId = i;
				samples.add(sample);
			}
			if (i % 100 == 99) {
				System.out.println(String.format(
						"Processed %d QAs, %d still left.",
						i + 1, questions.size() - i - 1));
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
		ArrayList<Object> objs =
				(ArrayList<Object>) istream.readObject();
		for (int i = 0; i < objs.size(); i++) {
			samples.add((QASample) objs.get(i));
		}
		istream.close();
		System.out.println(String.format("Loaded %d samples from %s.",
				samples.size(), filePath));
	}
	
	public void extractFeaturesAndLabels(
			AnswerIdFeatureExtractor featureExtractor) {
		int numSamples = samples.size(),
			numQuestions = questions.size();
		features = new Feature[numSamples][];
		labels = new double[numSamples];
		answerFlags = new int[numQuestions][];
		answerHeads = new int[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			answerFlags[i] = questions.get(i).answerFlags;
			answerHeads[i] = new int[answerFlags.length];
			Arrays.fill(answerHeads[i], -1);
		}			
		for (int i = 0; i < numSamples; i++) {
			QASample sample = samples.get(i);
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			features[i] = new Feature[fv.size()];
			int[] fids = Arrays.copyOf(fv.keys(), fv.size());
			Arrays.sort(fids);
			for (int j = 0; j < fids.length; j++) {
				// Liblinear feature id starts from 1.
				features[i][j] = new FeatureNode(fids[j] + 1, fv.get(fids[j]));
			}
			int label = (sample.isPositiveSample ? 1 : -1);
			labels[i] = label;
			answerHeads[sample.questionId][sample.answerHead] = label;
			
			if (i % 10000 == 9999) {
				System.out.println(String.format(
						"Extracted features for %d samples, %d still left.",
						i + 1, numSamples - i - 1));
			}
		}

	}

}
