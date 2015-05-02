package experiments;

import java.util.ArrayList;

import annotation.PropositionAligner;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import util.DataUtils;

/**
 * Not using the Crowdflower data format here.
 * 
 * @author luheng
 *
 */
public class CrowdFlowerPropIdDataRetriever {

	private static final String annotationFilePath =
			"crowdflower/CF_trial_propid_luheng.tsv";
	
	public static void readIdentifiedPropositions(
			ArrayList<SRLSentence> sentences,
			ArrayList<ArrayList<Proposition>> propositions) {
		assert (sentences != null);
		assert (propositions != null);
		
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
	
		ArrayList<ArrayList<String>> annotation = DataUtils.readTSVFile(
				annotationFilePath);
		
		PropositionAligner propAligner = new PropositionAligner();
		
		int numSentences = annotation.size() - 1;
		int[] sentIds = new int[numSentences];
		
		for (int i = 1; i < annotation.size(); i++) {
			ArrayList<String> row = annotation.get(i);
			
			int sentId = Integer.parseInt(row.get(0));
			sentIds[i - 1] = sentId;
			
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences
					.get(sentId);
			ArrayList<Proposition> props = new ArrayList<Proposition>();
			
			int lastMatched = 0;
			for (int j = 2; j < row.size(); j++) {
				String propString = row.get(j);
				Proposition aligned = propAligner.align(sentence, propString,
						lastMatched);
				if (aligned != null) {
					props.add(aligned);
					lastMatched = aligned.span[1];
				}
			}
			sentences.add(sentence);
			propositions.add(props);
		}
		assert (sentences.size() == propositions.size());
	}

}
