package util;

import java.util.ArrayList;

/* An easy solution to process CSV format files 
 * 
 */
public class CSVUtils {
	/* Split a line into array of strings by the comma.
	 * 
	 */
	 public static ArrayList<String> getColumns(String line) {
		 ArrayList<String> columns = new ArrayList<String>();
		 int length = line.length(),
		     lastQuote = -1,
		     lastComma = -1;
		 for (int i = 0; i < length; i++) {
			 char currChar = line.charAt(i);
			 if (currChar == '\"') {
				 if (i + 1 < length && line.charAt(i + 1) == '\"') {
					 continue;
				 }
				 if (lastQuote == -1) {
					 lastQuote = i;
				 } else {
					 columns.add(processQuotes(
							 line.substring(lastQuote + 1, i)));
					 lastQuote = -1;
					 lastComma = i + 1;
				 }
			 } else if (currChar == ',' && lastQuote == -1 && i > lastComma) {
				 // Not bracketed by quotes.
				 columns.add(processQuotes(
						 line.substring(lastComma + 1, i)));
				 lastComma = i;
			 } else if (i == length - 1) {
				 columns.add(processQuotes(
						 line.substring(lastComma + 1, i + 1)));
			 }
		 }
		 return columns;
	 }
	 
	 private static String processQuotes(String snippet) {
		 return snippet.replaceAll("\"\"", "\"").trim();
	 }
	 
	 public static void main(String[] args) {
		 // Test quotes processing.
		 String test1 =  "\"\"i like it!\"\"";
		 System.out.println(processQuotes(test1));
		 // Test csv processing.
		 String test2 = "i see, \"yes, no\",,, ok";
		 ArrayList<String> columns = getColumns(test2);
		 for (String column : columns) {
			 System.out.println(column);
		 }
	 }
}
