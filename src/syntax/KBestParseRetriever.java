package syntax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import util.StringUtils;
import data.AnnotatedSentence;
import data.QAPair;
import data.SRLCorpus;
import data.UniversalPostagMap;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
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
import evaluation.F1Metric;
import experiments.ExperimentUtils;
import experiments.XSSFDataRetriever;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.hash.TIntHashSet;

public class KBestParseRetriever {

	private static int kBest = 10;
	
	// Input: [1, 1, 1, 0, 0, 1, 1]
	// Output: [1, 1, 1, 0, 0, 2, 2] , each number represents a different span
	private static void getAnswerSpans(int[] qaFlags, int[] answerFlags,
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
	
	private static void generateTrainingSamples(
			SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences,
			UniversalPostagMap umap,
			ArrayList<QASample> trainingSamples) {
		LexicalizedParser lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                "-maxLength", "80",
                "-retainTmpSubcategories",
                "-printPCFGkBest", String.valueOf(kBest));
		
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

		int cnt = 0, numPosSamples = 0, numNegSamples = 0;
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
					// There is no lemma provided in stanford parser.
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
					int[] answerFlags = new int[qa.answerFlags.length];
					ArrayList<int[]> answerSpans = new ArrayList<int[]>();
					TIntHashSet answerHeads = new TIntHashSet();
					
					// Get answer spans.
					getAnswerSpans(qa.answerFlags, answerFlags, answerSpans);
					
					/*********** debug ************
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
									qa, idx, answerSpans,
									kBestScores, kBestParses,
									postags, lemmas));
							numPosSamples ++;
						} else if (!univPostags[idx].equals(".") &&
								idx != propHead) {
							trainingSamples.add(
									QASample.addNegativeSample(
										qa, idx, answerSpans,
										kBestScores, kBestParses,
										postags, lemmas));
							numNegSamples ++;
						}
					}
				}
			}
			cnt++;
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
	
	private static Feature[] getFeatures(TIntDoubleHashMap fv) {
		Feature[] feats = new Feature[fv.size()];
		int[] fids = Arrays.copyOf(fv.keys(), fv.size());
		Arrays.sort(fids);
		for (int i = 0; i < fids.length; i++) {
			feats[i] = new FeatureNode(fids[i] + 1, fv.get(fids[i]));
		}
		return feats;
	}

	public static void main(String[] args) {
		String xlsxFilePath =
				"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx";
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-trial");
		UniversalPostagMap umap = ExperimentUtils.loadPostagMap();
		HashMap<Integer, AnnotatedSentence> annotatedSentences =
				new HashMap<Integer, AnnotatedSentence>();
		ArrayList<QASample> samples = new ArrayList<QASample>();
		KBestFeatureExtractor featureExtractor =
				new KBestFeatureExtractor(trainCorpus, 3 /* min feature count */);
		
		try {
			XSSFDataRetriever.readXSSFAnnotations(xlsxFilePath,
					trainCorpus, annotatedSentences);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		ArrayList<Integer> sentIds = new ArrayList<Integer>();
		HashSet<Integer> trainIds = new HashSet<Integer>();
		double trainTestSplit = 0.7;
		
		for (int id : annotatedSentences.keySet()) {
			sentIds.add(id);
		}
		// TODO: shuffle sentence ids
		for (int i = 0; i < sentIds.size() * trainTestSplit; i++) {
			trainIds.add(sentIds.get(i));
		}
		
		generateTrainingSamples(trainCorpus, annotatedSentences, umap, samples);
		featureExtractor.extractFeatures(samples);
		featureExtractor.featureDict.prettyPrint();
		
		// ****************** Liblinear *******************		
		int numFeatures = featureExtractor.numFeatures();
		int numTrains = 0, numTests = 0;
		for (QASample sample : samples) {
			if (trainIds.contains(sample.sentence.sentenceID)) {
				numTrains ++;
			} else {
				numTests ++;
			}
		}
		System.out.println(String.format(
				"Number of training samples: %d. Number of test samples: %d.",
					numTrains, numTests));

		Feature[][] trains, tests;
		double[] trainLabels, testLabels;
		trains = new Feature[numTrains][];
		trainLabels = new double[numTrains];
		tests = new Feature[numTests][];
		testLabels = new double[numTests];
		
		numTrains = numTests = 0;
		for (QASample sample : samples) {
			TIntDoubleHashMap fv = featureExtractor.getFeatures(sample);
			double label = (sample.isPositiveSample ? 1.0 : -1.0);
			if (trainIds.contains(sample.sentence.sentenceID)) {
				trains[numTrains] = getFeatures(fv);
				trainLabels[numTrains++] = label;
			} else {
				tests[numTests] = getFeatures(fv);
				testLabels[numTests++] = label;
			}
		}
		
		// ************** Training ******************
		Problem training = new Problem();
		training.l = trains.length;
		training.n = featureExtractor.numFeatures();
		training.x = trains;
		training.y = trainLabels;
		
		SolverType solver = SolverType.L2R_LR;
		double C = 1.0,  eps = 0.01;
		Parameter parameter = new Parameter(solver, C, eps);
		Model model = Linear.train(training, parameter);
		/*
		File modelFile = new File("baseline.model");
		try {
			model.save(modelFile);
			model = Model.load(modelFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		// *************** Testing *******************
		int numMatched = 0, numPred = 0, numGold = 0;
		for (int i = 0; i < numTests; i++) {
			int pred = (int) Linear.predict(model, tests[i]);
			int gold = (int) testLabels[i];
			if (gold > 0 && pred > 0) {
				numMatched ++;
			}
			if (gold > 0) {
				numGold ++;
			}
			if (pred > 0) {
				numPred ++;
			}
		}
		F1Metric f1 = new F1Metric(numMatched, numGold, numPred);
		System.out.println(f1.toString());
	}
}
