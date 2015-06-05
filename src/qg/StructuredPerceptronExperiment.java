package qg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import util.StrUtils;
import config.DataConfig;
import config.QuestionIdConfig;
import learning.KBestParseRetriever;
import data.Corpus;

public class StructuredPerceptronExperiment {
	private QuestionIdConfig config;
	private Corpus baseCorpus; 

	protected QGenDataset trainSet;
	protected ArrayList<QGenDataset> testSets;
	
	private static final int maxNumIterations = 100;
	private static final double learningRate = 1e-3;
	private String getSampleFileName(QGenDataset ds) {
		return ds.datasetName + ".qg.k" + config.kBest + ".smp";
	}
	
	public StructuredPerceptronExperiment() throws IOException {
		config = new QuestionIdConfig();
		baseCorpus = new Corpus("qa-exp-corpus");
		testSets = new ArrayList<QGenDataset>();

		System.out.println(config.toString());

		// ********** Config and load QA Data ********************
		trainSet = new QGenDataset(baseCorpus,
				StrUtils.join("_", config.trainSets));
		for (String dsName : config.trainSets) {
			trainSet.loadData(DataConfig.getDataset(dsName));
		}
		for (String dsName : config.testSets) {
			QGenDataset ds = new QGenDataset(baseCorpus, dsName);
			ds.loadData(DataConfig.getDataset(dsName));
			testSets.add(ds);
		}
		
		// **************** Load samples ****************
		if (config.regenerateSamples) {
			KBestParseRetriever syntaxHelper =
					new KBestParseRetriever(config.kBest);
			trainSet.generateSamples(syntaxHelper);
			for (QGenDataset ds : testSets) {
				ds.generateSamples(syntaxHelper);
			}
			// Cache qaSamples to file because parsing is slow.
			ObjectOutputStream ostream = null;
			try {
				ostream = new ObjectOutputStream(
						new FileOutputStream(getSampleFileName(trainSet)));
				ostream.writeObject(trainSet.samples);
				ostream.flush();
				ostream.close();
				for (QGenDataset ds : testSets) {
					ostream = new ObjectOutputStream(
							new FileOutputStream(getSampleFileName(ds)));
					ostream.writeObject(ds.samples);
					ostream.flush();
					ostream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
     			trainSet.loadSamples(getSampleFileName(trainSet));
     			for (QGenDataset ds : testSets) {
					ds.loadSamples(getSampleFileName(ds));
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void run() {
		StructuredPerceptron sp = new StructuredPerceptron(baseCorpus, trainSet,
				testSets);
		sp.run(maxNumIterations, learningRate);
	}
	
	public static void main(String[] args) {
		StructuredPerceptronExperiment exp = null;
		try {
			exp = new StructuredPerceptronExperiment();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		exp.run();
	}
}
