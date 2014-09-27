package experiments;

import java.io.IOException;

import data.DepCorpus;

public class DataPreparationExperiment {

	public static final String trainFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-train.conll";
	public static final String devFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-dev.conll";
	public static final String testFilename =
			"/Users/luheng/data/stanford-universal-dependencies/en-univiersal-test.conll";
	
	public static void main(String[] args) {
		DepCorpus trainCorpus = new DepCorpus();
		try {
			trainCorpus.loadCoNLL(trainFilename);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

}
