package experiments;

import java.io.Console;

import data.DepCorpus;
import data.DepSentence;
import decoding.Decoder;
import decoding.ViterbiDecoder;

/**
 * Interactive annotation using the workflow:
 * W-Rank -> Q-Gen -> A-Align -> Update
 * @author luheng
 *
 */
public class InteractiveAnnotationExperiment {
	
	private static DepCorpus trainCorpus = null;
	// private static ArrayList<AnnotatedSentence> annotatedSentences = null;
	private static Decoder decoder = null;
	
	private static InteractiveConsole console = null;
	private static int[] sentenceIDs = new int[] {6251, 9080, 8241, 8828, 55};
	
	private static int[] wordRanks;
	
	private static void annotateSentence(DepSentence sentence) {
		while (true) {
			// Compute word ranks.
			
			// Suggest word + wh-word combination.
			
			// Align answer.
			
			// Update.
		}
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadDepCorpus();
		decoder = new ViterbiDecoder();
		// Do not load annotation yet, haha.
				
		// Interaction. Try the first sentence.
		console = new InteractiveConsole();
		annotateSentence(trainCorpus.sentences.get(sentenceIDs[0]));
	}
}
