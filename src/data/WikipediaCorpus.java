package data;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import annotation.PTBTokens;

public class WikipediaCorpus extends Corpus {

	public HashMap<Integer, WikipediaDocInfo> wikiInfo;
	public ArrayList<Integer> docIds, paragraphIds, sentenceIds;
	
	private static final String wikiFilePrefix = "wiki_";
	private static final int maxNumFilesPerDir = 100;
	
	public int minSentenceLength = 10;
	public int maxSentenceLength = 60;
	public int maxNumSentences = 3000;
	public int maxAllowedNesting = 1;
	public int maxAllowedBrackets = 5;
	public int randomSeed = 123456;
	public double sampleRate = 0.0005;
	
	private static HashMap<String, String> bracketMap;
	static {
		bracketMap = new HashMap<String, String>();
		bracketMap.put("(", ")");
		bracketMap.put("[", "]");
		bracketMap.put("{", "}");
		bracketMap.put("``", "''");
		bracketMap.put("`", "'");
	}
	
	public WikipediaCorpus(String corpusName) {
		super(corpusName);
		wikiInfo = new HashMap<Integer, WikipediaDocInfo>();
		docIds = new ArrayList<Integer>();
		paragraphIds = new ArrayList<Integer>();
		sentenceIds = new ArrayList<Integer>();
	}
	
	private boolean preTest(String str) {
		//return !str.matches("^[\u0000-\u0080]+$");
		// Only keeping paragraphs:
		// 1). Contains English character only.
		// 2). Starts with an uppercase letter or the character ".
		return str.matches("^[\"A-Z][\u0000-\u0080]+$");
	}
	
	private String removeEmptyBrackets(String str) {
		String ret = str;
		for (int i = 0; i < 10; i++) {			
			String ret1 = ret.replaceAll("``[^`']*''", "")
					.replaceAll("\\([^\\(\\)]*\\)", "")
					.replaceAll("\\[[^\\[\\]]*\\]", "")
					.replaceAll("\\{[^\\{\\}]*\\}", "");
			if (ret1.equals(ret)) {
				break;
			}
			ret = ret1;
		}
		return ret;
	}

	private boolean bracketingChecker(Sentence sentence) {
		TObjectIntHashMap<String> stack = new TObjectIntHashMap<String>();
		int numNesting = 0, maxNumNesting = 0, numBrackets = 0;
		for (int i = 0; i < sentence.length; i++) {
			String token = sentence.getTokenString(i);
			if (bracketMap.keySet().contains(token)) {
				stack.adjustOrPutValue(bracketMap.get(token), 1, 1);
				maxNumNesting = Math.max(maxNumNesting, ++numNesting);
			} else if (bracketMap.values().contains(token)) {
				stack.adjustOrPutValue(token, -1, -1);
				if (stack.get(token) < 0) {
					// Unmatched brackets.
					return false;
				} else if (stack.get(token) == 0) {
					++ numBrackets;
				}
				-- numNesting;
			}
		}
		return (maxNumNesting <= maxAllowedNesting) &&
				(numBrackets <= maxAllowedBrackets);
	}
	
	private boolean postTest(Sentence sentence) {
		if (sentence.length < minSentenceLength ||
			sentence.length > maxSentenceLength ||
			sentence.containsQuestion()) {
			return false;
		}
		if (!bracketingChecker(sentence)) {
			return false;
		}
		String lastToken = sentence.getTokenString(sentence.length - 1);
		if (!lastToken.equals(".") && !lastToken.equals("!") &&
			!lastToken.equals("''")) {
			return false;
		}
		return true;
	}

	private boolean sampled(Random random) {
		return random.nextDouble() < sampleRate;
	}
	
	public void sampleFromWikipedia(String baseFilePath)
			throws NullPointerException, IOException {		
		int totalNumSentences = 0, totalNumDocs = 0;

		File file = new File(baseFilePath);
		BufferedReader reader = null;
		WikipediaDocInfo docInfo = null;
		Random random = new Random(randomSeed);
		
		for (String dirPath : file.list()) {
			File dir = new File(baseFilePath + "/" + dirPath);
			if (!dir.isDirectory()) {
				continue;
			}
			for (int fid = 0; fid < maxNumFilesPerDir; fid ++) {
				String fname = String.format("%s/%s%02d",
						dir.getAbsolutePath(), wikiFilePrefix, fid);
				if (fid == 99) {
					System.out.println(String.format(
							"Processing %s ... current # sentences: %d",
								fname, sentences.size()));
				}
				try {
					reader = new BufferedReader(new InputStreamReader(
							new FileInputStream(fname), "UTF8"));
				} catch (FileNotFoundException e) {
				//	e.printStackTrace();
					continue;
				}
				
				String currLine = null;
				int docId = -1, paragraphId = -1, sentenceId = -1;
				while ((currLine = reader.readLine()) != null) {
					if (currLine.isEmpty() || currLine.startsWith("</doc>")) {
						continue;
					}
					if (currLine.startsWith("<doc id=")) {
						totalNumDocs ++;
						if (!(docInfo == null)) {
							docInfo.sentIdSpan[1] = sentences.size();
							wikiInfo.put(docInfo.docId, docInfo);
						}
						docInfo = new WikipediaDocInfo(currLine);
						docInfo.sentIdSpan[0] = sentences.size();
						docId = docInfo.docId;
						paragraphId = -1;
						continue;
					}
					
					paragraphId ++;
					sentenceId = -1;
				
					if (!preTest(currLine)) {
						continue;
					}
					
					currLine = removeEmptyBrackets(currLine);
					
					if (!sampled(random)) {
						continue;
					}

					DocumentPreprocessor dp = new DocumentPreprocessor(
							new StringReader(currLine));
					for (List<HasWord> rawSentence : dp) {
						sentenceId ++;
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
						Sentence sentence = new Sentence(tokenIds.toArray(),
								this, sentId);
						if (postTest(sentence)) {
							sentences.add(sentence);
							docIds.add(docId);
							paragraphIds.add(paragraphId);
							sentenceIds.add(sentenceId);
						}
					}
				}
				reader.close();
				if (sentences.size() > maxNumSentences) {
					break;
				}
			}
		}
		
		System.out.println(String.format("Processed %d documents.",
				totalNumDocs));
		System.out.println(String.format("Read %d sentences, kept %d.",
				totalNumSentences, sentences.size()));
		/*
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("wiki_sampled.txt"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < sentences.size(); i++) {
			System.out.println(docIds.get(i) + "\t" + paragraphIds.get(i) + "\t" 
					+ sentences.get(i).getTokensString());
		}
		*/
	}
	
	public void loadFromWikiMetaFile(String filePath) throws IOException {
		BufferedReader reader = new BufferedReader(
				new FileReader(new File(filePath)));
		String currLine = "";
		while ((currLine = reader.readLine()) != null) {
			String[] info = currLine.split("\t");
			int docId = Integer.parseInt(info[0]);
			// String docTitle = info[1].trim();
			int paraId = Integer.parseInt(info[2]);
			int sentInPara = Integer.parseInt(info[3]);
			String[] tokens = info[4].split("\\s+");
			int[] tokenIds = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				tokenIds[i] = wordDict.addString(tokens[i]);
			}
			int sentId = sentences.size();
			sentences.add(new Sentence(tokenIds, this, sentId));
			docIds.add(docId);
			paragraphIds.add(paraId);
			sentenceIds.add(sentInPara);
			// TODO: Wiki doc info.
		}
		reader.close();
		System.out.println(String.format("Read %d sentences from %s",
				sentences.size(), filePath));
	}
}
