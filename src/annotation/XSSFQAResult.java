package annotation;

import java.util.ArrayList;

public class XSSFQAResult {
	public int sentenceId, propHead;
	public String proposition;
	
	// Actual annotation result.
	public ArrayList<String[]> questions; // multiple slots of each question
	public ArrayList<String[]> answers;   // multiple versions of each answer
	public ArrayList<String> note;
	
}
