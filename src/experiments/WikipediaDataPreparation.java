package experiments;

import java.io.IOException;

import data.WikipediaCorpus;

public class WikipediaDataPreparation {

	public static void main(String[] args) {
		String inputPath = args[0];
			   
		WikipediaCorpus wikiCorpus = new WikipediaCorpus("wikipedia-all");
		try {
			wikiCorpus.loadWikipediaData(inputPath);
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		
		PosTaggerVerbIdentifier verbId = new PosTaggerVerbIdentifier(wikiCorpus);
		
	}
}
