package experiments;

import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import annotation.CrowdFlowerQAResult;

public class CrowdflowerQADataRetriever {

	private static final String annotationFilePath =
			"crowdflower/CF_QA_trial_s20_result.csv";
	
	public static void readAnnotationResult() throws IOException {
		FileReader fileReader = new FileReader(annotationFilePath);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader()
				.parse(fileReader);
		
		for (CSVRecord record : records) {
			//int sentenceId = Integer.parseInt(record.get("sent_id"));
			CrowdFlowerQAResult qa = CrowdFlowerQAResult.parseCSV(record);
			System.out.println(qa.toString());
		}
		fileReader.close();
	}
	
	public static void main(String[] args) {
		//SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
		//		ExperimentUtils.conll2009TrainFilename, "en-srl-train");
	
		try {
			readAnnotationResult();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
