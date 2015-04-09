package syntax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import util.StringUtils;
import data.AnnotatedSentence;
import data.QAPair;
import data.SRLCorpus;
import data.UniversalPostagMap;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.LexicalizedParserQuery;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ScoredObject;
import experiments.ExperimentUtils;
import experiments.XSSFDataRetriever;

public class KBestParseRetriever {

	private static int kBest = 10;
	
	// Input: [1, 1, 1, 0, 0, 1, 1]
	// Output: [1, 1, 1, 0, 0, 2, 2] , each number represents a different span
	private static int[] getAnswerSpans(int[] answerFlags) {
		int label = 0;
		int[] spans = new int[answerFlags.length];
		for (int i = 0; i < answerFlags.length; i++) {
			if (answerFlags[i] == 0) {
				spans[i] = 0;
			} else if (i == 0 || answerFlags[i - 1] == 0) {
				spans[i] = ++label;
			} else {
				spans[i] = label;
			}
		}
		return spans;
	}
	
	// TODO
	
	
	private static void test(SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences,
			UniversalPostagMap umap) {
		LexicalizedParser lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                "-maxLength", "80",
                "-retainTmpSubcategories",
                "-printPCFGkBest", String.valueOf(kBest));
		
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

		ArrayList<QASample> trainingSamples = new ArrayList<QASample>();
		
		int cnt = 0;
		for (AnnotatedSentence sent : annotatedSentences.values()) {
			//System.out.println(sent.sentence.getTokensString());
			String[] tokens = sent.sentence.getTokensString().split("\\s+");
			LexicalizedParserQuery lpq = (LexicalizedParserQuery) lp.parserQuery();
			lpq.parse(Sentence.toWordList(tokens));
			List<ScoredObject<Tree>> parses = lpq.getKBestPCFGParses(kBest);
			ArrayList<Collection<TypedDependency>> kBestParses =
					new ArrayList<Collection<TypedDependency>>();
			ArrayList<Double> kBestScores = new ArrayList<Double>();
			String[] postags = new String[sent.sentence.length],
					 lemmas = new String[sent.sentence.length];
			
			for (int i = 0; i < parses.size(); i++) {
				kBestScores.add(parses.get(i).score());
				GrammaticalStructure gs =
						gsf.newGrammaticalStructure(parses.get(i).object());
				Collection<TypedDependency> deps =
						gs.typedDependenciesCCprocessed();
				kBestParses.add(deps);
				if (i == 0) {
					// Get postags and lemmas from one-best.
					ArrayList<TaggedWord> tags =
							parses.get(i).object().taggedYield();
					assert (tags.size() == sent.sentence.length);
					for (int j = 0;  j < tags.size(); j++) {
						String postag = tags.get(j).tag();
						postags[j] = umap.getUnivPostag(postag);
					}
					/*
					for (TypedDependency dep : deps) {
						int wid = dep.dep().index() - 1;
						if (wid >= 0) {
							lemmas[wid] = dep.dep().lemma();
						}
					}
					*/
				}
			}

			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					int[] answerSpans = getAnswerSpans(qa.answerFlags);
					HashSet<Integer> answerHeads =
							new HashSet<Integer>();
					// Get answer heads
					for (ScoredObject<Tree> parse : parses) {
						GrammaticalStructure gs = gsf.newGrammaticalStructure(parse.object());
						Collection<TypedDependency> deps = gs.typedDependenciesCCprocessed();
		
						for (TypedDependency dep : deps) {
							int head = dep.gov().index() - 1,
								arg = dep.dep().index() - 1;
							if (answerSpans[arg] > 0 && (head < 0 ||
									answerSpans[head] != answerSpans[arg])) {
								answerHeads.add(arg);
							}
						}
					}
					//System.out.print(qa.toString() + "\t");)
					for (int answerHead : answerHeads) {
						trainingSamples.add(
							QASample.addPositiveSample(
								qa, answerHead,
								kBestScores, kBestParses,
								postags, lemmas));
					}
				}
			}
			
			if (++cnt >= 10) {
				break;
			}
		}
		
		for (QASample sample : trainingSamples) {
			System.out.println(sample  + "\n");
		}
	}
	
	

	public static void main(String[] args) {
		String xlsxFilePath =
				"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx";
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-trial");
		UniversalPostagMap umap = ExperimentUtils.loadPostagMap();
		
		HashMap<Integer, AnnotatedSentence> annotatedSentences =
				new HashMap<Integer, AnnotatedSentence>();
		try {
			XSSFDataRetriever.readXSSFAnnotations(xlsxFilePath,
					trainCorpus, annotatedSentences);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		test(trainCorpus, annotatedSentences, umap);
	}
}
