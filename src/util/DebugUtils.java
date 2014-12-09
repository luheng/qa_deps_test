package util;

import data.SRLSentence;

public class DebugUtils {

	public static void printSemanticArcs(SRLSentence sentence,
			String[][] arcs) {
		for (int i = 0; i < arcs.length; i++) {
			int counter = 0;
			for (int j = 0; j < arcs[i].length; j++) {
				if (!arcs[i][j].isEmpty()) {
					String t1 = (i == 0 ? "[root]" :
						sentence.getTokenString(i - 1));
					String t2 = sentence.getTokenString(j - 1);
					System.out.print(String.format("%s->%s: %s\t",
							t1, t2, arcs[i][j]));
					counter ++;
				}
			}
			if (counter > 0) {
				System.out.println();
			}
		}
		System.out.println();
	}
}
