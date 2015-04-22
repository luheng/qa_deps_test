package learning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import data.AnnotatedSentence;
import data.Corpus;
import data.QAPair;
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
import gnu.trove.set.hash.TIntHashSet;

public class KBestParseRetriever {
	int kBest;
	LexicalizedParser lp;
	TreebankLanguagePack tlp;
	GrammaticalStructureFactory gsf;
	UniversalPostagMap umap;
	
	// ***** Cached syntax ***********
	int cachedSentenceId;
	ArrayList<Collection<TypedDependency>> kBestParses = null;
	ArrayList<Double> kBestScores = null;
	String[] postags = null;
	String[] univPostags = null;
	String[] lemmas = null;
	
	public KBestParseRetriever(int kBest) {
		this.kBest = kBest;
		this.lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                "-maxLength", "80",
                "-retainTmpSubcategories",
                "-printPCFGkBest", String.valueOf(kBest),
                "-maxLength", "100");
		
		this.tlp = new PennTreebankLanguagePack();
		this.gsf = tlp.grammaticalStructureFactory();
		this.umap = ExperimentUtils.loadPostagMap();
		
		this.cachedSentenceId = -1;
	}
	
	public ArrayList<QASample> generateSamplesGivenQuestion(QAPair qa) {
		if (cachedSentenceId != qa.sentence.sentenceID) {
			retrieveAndCacheSyntax(qa.sentence);
		}		

		int[] answerFlags = new int[qa.answerFlags.length];
		ArrayList<int[]> answerSpans = new ArrayList<int[]>();
		TIntHashSet answerHeads = new TIntHashSet();
		getAnswerSpans(qa.answerFlags, answerFlags, answerSpans);
		
		for (Collection<TypedDependency> deps : kBestParses) {
			for (TypedDependency dep : deps) {
				int head = dep.gov().index() - 1,
					arg = dep.dep().index() - 1;
				if (answerFlags[arg] > 0 && (head < 0 ||
						answerFlags[head] != answerFlags[arg])) {
					answerHeads.add(arg);
				}
			}
		}
		
		ArrayList<QASample> samples = new ArrayList<QASample>();
		for (int idx = 0; idx < qa.sentence.length; idx++) {
			if (answerHeads.contains(idx)) {
				samples.add(
					QASample.addPositiveSample(
						qa.sentence, qa.propHead, qa.questionId,
						qa.questionWords, idx,
						kBestScores, kBestParses,
						postags, lemmas));
			} else if (!univPostags[idx].equals(".") &&
					idx != qa.propHead) {
				samples.add(
					QASample.addNegativeSample(
							qa.sentence, qa.propHead, qa.questionId,
							qa.questionWords, idx,
							kBestScores, kBestParses,
							postags, lemmas));
			}
		}
		return samples;
	}
	
	private void retrieveAndCacheSyntax(data.Sentence sentence) {
		String[] tokens = sentence.getTokensString().split("\\s+");
		
		LexicalizedParserQuery lpq = (LexicalizedParserQuery) lp.parserQuery();
		lpq.parse(Sentence.toWordList(tokens));
		
		List<ScoredObject<Tree>> parses = lpq.getKBestPCFGParses(kBest);
		
		kBestParses = new ArrayList<Collection<TypedDependency>>();
		kBestScores = new ArrayList<Double>();
		postags = new String[sentence.length];
		univPostags = new String[sentence.length];
		lemmas = new String[sentence.length];
		
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
				assert (tags.size() == sentence.length);
				for (int j = 0;  j < tags.size(); j++) {
					postags[j] = tags.get(j).tag();
					univPostags[j] = umap.getUnivPostag(postags[j]);
				}
				// TODO: add lemma
			}
		}
		cachedSentenceId = sentence.sentenceID;
	}
	
	// Input: [1, 1, 1, 0, 0, 1, 1]
	// Output: [1, 1, 1, 0, 0, 2, 2] , each number represents a different span
	private void getAnswerSpans(int[] qaFlags, int[] answerFlags,
			ArrayList<int[]> answerSpans) {
		int label = 0, spanStart = -1;
		for (int i = 0; i < qaFlags.length; i++) {
			if (spanStart >= 0 && (qaFlags[i] == 0 || qaFlags[i - 1] == 0)) {
				answerSpans.add(new int[] {spanStart, i});
			}
			if (qaFlags[i] == 0) {
				answerFlags[i] = 0;
				spanStart = -1;
			} else if (i == 0 || qaFlags[i - 1] == 0) {
				answerFlags[i] = ++label;
				spanStart = i;
			} else {
				answerFlags[i] = label;
			}
		}
		if (spanStart >= 0) {
			answerSpans.add(new int[] {spanStart, qaFlags.length});
		}
	}
	
	public void generateTrainingSamples(
			Corpus corpus,
			Collection<AnnotatedSentence> annotations,
			UniversalPostagMap umap,
			int kBest,
			ArrayList<QASample> trainingSamples) {
		
		int cnt = 0, numPosSamples = 0, numNegSamples = 0;
		for (AnnotatedSentence sent : annotations) {
			//System.out.println(sent.sentence.getTokensString());
			String[] tokens = sent.sentence.getTokensString().split("\\s+");
			LexicalizedParserQuery lpq = (LexicalizedParserQuery) lp.parserQuery();
			lpq.parse(Sentence.toWordList(tokens));
			List<ScoredObject<Tree>> parses = lpq.getKBestPCFGParses(kBest);
			ArrayList<Collection<TypedDependency>> kBestParses =
					new ArrayList<Collection<TypedDependency>>();
			ArrayList<Double> kBestScores = new ArrayList<Double>();
			String[] postags = new String[sent.sentence.length],
					 univPostags = new String[sent.sentence.length],
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
						postags[j] = tags.get(j).tag();
						univPostags[j] = umap.getUnivPostag(postags[j]);
					}
					// TODO: add lemma
				}
			}
			
			// debug
			/*
			System.out.println(sent.sentence.getTokensString());
			System.out.println(StringUtils.join(" ", postags));
			System.out.println(StringUtils.join(" ", univPostags));
			*/
			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					int[] answerFlags = new int[qa.answerFlags.length];
					ArrayList<int[]> answerSpans = new ArrayList<int[]>();
					TIntHashSet answerHeads = new TIntHashSet();
					
					// Get answer spans.
					getAnswerSpans(qa.answerFlags, answerFlags, answerSpans);
					
					// *********** debug ************
					/*
					System.out.println(StringUtils.intArrayToString("\t", qa.answerFlags));
					System.out.println(StringUtils.intArrayToString("\t", answerFlags));
					System.out.println(qa.getAnswerString());
					for (int[] span : answerSpans) {
						System.out.print(sent.sentence.getTokenString(span) + " ... ");
					}
					System.out.println("\n");
					*/
					
					// Get answer heads
					for (ScoredObject<Tree> parse : parses) {
						GrammaticalStructure gs = gsf.newGrammaticalStructure(parse.object());
						Collection<TypedDependency> deps = gs.typedDependenciesCCprocessed();
						for (TypedDependency dep : deps) {
							int head = dep.gov().index() - 1,
								arg = dep.dep().index() - 1;
							if (answerFlags[arg] > 0 && (head < 0 ||
									answerFlags[head] != answerFlags[arg])) {
								answerHeads.add(arg);
							}
						}
					}
					
					for (int idx = 0; idx < sent.sentence.length; idx++) {
						if (answerHeads.contains(idx)) {
							trainingSamples.add(
								QASample.addPositiveSample(
									qa.sentence, propHead, -1,
									qa.questionWords, idx,
									kBestScores, kBestParses,
									postags, lemmas));
							numPosSamples ++;
						} else if (!univPostags[idx].equals(".") &&
								idx != propHead) {
							trainingSamples.add(
									QASample.addNegativeSample(
										qa.sentence, propHead, -1,
										qa.questionWords, idx,
										kBestScores, kBestParses,
										postags, lemmas));
							numNegSamples ++;
						}
					}
				}
			}
			if (++cnt % 10 == 0) {
				System.out.println(String.format("Processed %d sentences", cnt));
			}
		}
		/*
		for (QASample sample : trainingSamples) {
			System.out.println(sample  + "\n");
		}
		*/
		System.out.println(String.format(
				"Extracted %d samples. %d positive, %d negative",
					trainingSamples.size(), numPosSamples, numNegSamples));
	}
}
