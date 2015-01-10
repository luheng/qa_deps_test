package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	/**
	 * Easy hack to read TSV files. No hastles.
	 * @return
	 */
	public static ArrayList<ArrayList<String>> readTSVFile(String filePath) {
		ArrayList<ArrayList<String>> info = new ArrayList<ArrayList<String>>();
		BufferedReader reader;
		
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filePath)));
			String line;
			while ((line = reader.readLine()) != null) {
				ArrayList<String> currInfo = new ArrayList<String>();
				if (!line.trim().isEmpty()) {
					String[] strs = line.split("\t");
					for (String s : strs) {
						currInfo.add(s.trim());
					}
					info.add(currInfo);
				}
			}
			reader.close();
		} catch (Exception e) {
		}
		return info;
	}
}
