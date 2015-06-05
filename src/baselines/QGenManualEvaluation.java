package baselines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class QGenManualEvaluation {

	//private static String evalFilePath = "./data/qgen_newswire.tsv";
	private static String evalFilePath = "./data/qgen_wikipedia.tsv";
	
	
	private static void evaluate() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				new File(evalFilePath)));
		int lineCnt = 0, verbCnt = 0;
		String line = "";
		
		int[] answerable = new int[5],
			  grammatical = new int[5],
			  understandable = new int[5],
			  ndiff = new int[5];
		
		Arrays.fill(answerable, 0);
		Arrays.fill(grammatical, 0);
		Arrays.fill(understandable, 0);
		Arrays.fill(ndiff, 0);
		
		int qid = 0;
		
		while ((line = reader.readLine()) != null) {
			String[] info = line.trim().split("\t");
			if (info.length > 0 && info[0].startsWith("Q")) {
				if (info[0].equals("Q0")) {
					qid = 0;
				}
				System.out.println(qid + "\t" + line);
				int ans = info[2].equals("y") ? 1 : 0;
				int gra = info[3].equals("y") ? 1 : 0;
				int und = info[4].equals("y") ? 1 : 0;
				int dif = (ans > 0 && (info.length <= 5 ||
						info[5].trim().isEmpty())) ? 1 : 0;
				
				for (int j = qid; j < 5; j++) {
					answerable[j] += ans;
					if (ans == 0 || dif > 0) {
						grammatical[j] += gra;
					}
					if (ans == 0 || dif > 0) {
						understandable[j] += und;
					}
					ndiff[j] += dif;
				}
				qid ++;
			}
			if (++lineCnt % 8 == 0) {
				++ verbCnt;
			}
		}
		reader.close();
		
		for (int j = 0; j < 5; j++) {
			System.out.print("prec@" + (j+1) + "\t");
//			System.out.print(100.0 * answerable[j] / verbCnt / (j+1) + "\t");
			System.out.print(100.0 * grammatical[j] / verbCnt / (j+1) + "\t");
//			System.out.print(100.0 * understandable[j] / verbCnt / (j+1) + "\t");
			System.out.print(100.0 * ndiff[j] / verbCnt / (j+1) + "\t");
			System.out.println();
		}
	}
	
	public static void main(String[] args) {
		try {
			evaluate();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
