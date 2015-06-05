package learning;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
import data.Sentence;


public class QGenDataset extends QADataset {
	
	public QGenDataset(Corpus corpus, String name) {
		super(corpus, name);
	}
	
	public QGenDataset(Corpus corpus) {
		super(corpus);
	}
	
	public void generateSamples(KBestParseRetriever syntaxHelper) {
		// For each <sentence, target> pair, generate a set of samples
		int numTargetWords = 0, numPositiveSamples = 0;
		for (AnnotatedSentence annotSent : sentences) {
			Sentence sent = annotSent.sentence;
			for (int propHead : annotSent.qaLists.keySet()) {
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					QASample sample = syntaxHelper.generateQGenSample(
							sent, propHead, qa);
					samples.add(sample);
				}
			}
			if (numTargetWords++ % 100 == 99) {
				System.out.print(String.format(
						"Processed %d Sentences.\t",
						numTargetWords + 1));
				System.out.println(String.format(
						"Generated %d samples. %d positive, %d negative.",
						samples.size(), numPositiveSamples,
						samples.size() - numPositiveSamples));
			}
		}
		System.out.println(String.format(
				"Generated %d samples. %d positive, %d negative.",
				samples.size(), numPositiveSamples,
				samples.size() - numPositiveSamples));
	}
}

