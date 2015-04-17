package data;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.List;

import annotation.PTBTokens;

public class WikipediaCorpus extends Corpus {

	HashMap<Integer, WikipediaDocInfo> wikiInfo;
	private static final String wikiFilePrefix = "wiki_";
	private static final int maxNumFilesPerDir = 100;
	
	public WikipediaCorpus(String corpusName) {
		super(corpusName);
	}
/*
	private boolean containsNonEnglishCharacters(String str) {
		return !str.matches("^[\u0000-\u0080]+$");
	}
*/
	
	public void loadWikipediaData(String baseFilePath)
			throws NullPointerException, IOException {		
		int totalNumSentences = 0;

		File file = new File(baseFilePath);
		for (String dirPath : file.list()) {
			File dir = new File(baseFilePath + "/" + dirPath);
			if (!dir.isDirectory()) {
				continue;
			}
			for (int fid = 0; fid < maxNumFilesPerDir; fid ++) {
				String fname = String.format("%s/%s%02d",
						dir.getAbsolutePath(), wikiFilePrefix, fid);
				System.out.println(String.format("Processing %s ...", fname));
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(fname), "UTF8"));
				
				String currLine = null;
				int docId = -1, paragraphId = -1;
				while ((currLine = reader.readLine()) != null) {
					if (currLine.isEmpty() || currLine.startsWith("</doc>")) {
						continue;
					}
					if (currLine.startsWith("<doc id=")) {
						// TODO: parse info from:
						// <doc id="358" url="http://en.wikipedia.org/wiki?curid=358" title="Algeria">
						paragraphId = 0;
						continue;
					}
					paragraphId ++;
					// Only keeping paragraphs:
					// 1). Contains English character only.
					// 2). Starts with an uppercase letter or the character ".
					if (!currLine.matches("^[\"A-Z][\u0000-\u0080]+$")) {
					//	System.out.println(currLine);
						continue;
					}
					currLine = currLine.replace("( )", "").replace("{ }", "")
							.replace("[ ]", "");

					DocumentPreprocessor dp = new DocumentPreprocessor(
							new StringReader(currLine));
					for (List<HasWord> rawSentence : dp) {
						totalNumSentences ++;
						TIntArrayList tokenIds = new TIntArrayList();
						for (HasWord word : rawSentence) {
							String token = word.word().trim();
							if (PTBTokens.tokenMap.containsKey(token)) {
								token = PTBTokens.tokenMap.get(token);
							}						
							tokenIds.add(wordDict.addString(token));
						}
						int sentId = sentences.size();
						Sentence sentence = new Sentence(tokenIds.toArray(), this, sentId);
						// TODO: remove sentences with non-matching brackets, 
						// quotations, and those did not end with a punctuation.
						if (tokenIds.size() < 10 || tokenIds.size() > 100 ||
							sentence.containsQuestion()) {
							continue;
						}
						
						sentences.add(sentence);
			//			System.out.println(paragraphId + "\t" + sentence.getTokensString());
					}
				}
				reader.close();
			}
		}
		/*
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("wiki-AA.txt"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/

		
		System.out.println(String.format("Read %d sentences, kept %d.",
				totalNumSentences, sentences.size()));
	}
	
}
