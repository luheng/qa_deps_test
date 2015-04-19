package experiments;

import java.io.IOException;
import java.util.ArrayList;

import data.Sentence;
import data.WikipediaCorpus;

public class WikipediaDataPreparation {

	public static void main(String[] args) {
		String inputPath = args[0];
			   
		WikipediaCorpus wikiCorpus = new WikipediaCorpus("wikipedia-all");
	//	DepCorpus taggedCorpus = new DepCorpus("tagged-wikipedia-all");
		ArrayList<ArrayList<Integer>> extractedProps = new ArrayList<ArrayList<Integer>>();
		
		try {
			wikiCorpus.loadWikipediaData(inputPath);
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Start tagging " + wikiCorpus.sentences.size() + " sentences ...");
		
		PosTaggerVerbIdentifier verbId = new PosTaggerVerbIdentifier(wikiCorpus);
		for (int i = 0; i < wikiCorpus.sentences.size(); i++) {
			Sentence sentence = wikiCorpus.sentences.get(i);
			//String sentStr = sentence.getTokensString();
			ArrayList<Integer> props = verbId.extractContentVerbs(sentence);
			System.out.println(sentence.getTokensString());
			for (int prop : props) {
				System.out.println("\t" + sentence.getTokenString(prop));
			}
			extractedProps.add(props);
		}
	}
}
