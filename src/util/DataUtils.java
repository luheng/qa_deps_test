package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import data.AnnotatedSentence;
import data.DepSentence;
import data.QAPair;

public class DataUtils {

	public static void saveAnnotatedSentences(
			ArrayList<AnnotatedSentence> annotatedSentences,
			String outputFileName) throws IOException {
		File file =  new File(outputFileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				file.getAbsoluteFile())); 
		
		for (AnnotatedSentence sentence : annotatedSentences) {
			DepSentence sent = sentence.depSentence;
			writer.write(sent.sentenceID + "\t" + sent.getTokensString()+ "\n");
			for (QAPair qa : sentence.qaList) {
				writer.write(qa.getQuestionString() + " ### " +
							 qa.getAnswerString() + "\n");
			}
		}
		writer.close();
	}
}
