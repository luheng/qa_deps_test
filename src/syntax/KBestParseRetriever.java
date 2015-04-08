package syntax;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import data.AnnotatedSentence;
import data.SRLCorpus;
import edu.stanford.nlp.ling.Sentence;
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

	private static int kBest = 5;
	
	// TODO: get kbest parses from stanford parser
	private static void test(SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences) {
		LexicalizedParser lp = LexicalizedParser.loadModel(
				"edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                "-maxLength", "80",
                "-retainTmpSubcategories",
                "-printPCFGkBest", String.valueOf(kBest));
		
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

		int cnt = 0;
		for (AnnotatedSentence sent : annotatedSentences.values()) {
			String[] tokens = sent.sentence.getTokensString().split("\\s+");
			LexicalizedParserQuery lpq = (LexicalizedParserQuery) lp.parserQuery();
			lpq.parse(Sentence.toWordList(tokens));
			List<ScoredObject<Tree>> parses = lpq.getKBestPCFGParses(kBest);
			for (ScoredObject<Tree> parse : parses) {
				System.out.println(parse.score());
				GrammaticalStructure gs =
						gsf.newGrammaticalStructure(parse.object());
				Collection<TypedDependency> deps =
						gs.typedDependenciesCCprocessed();
				System.out.println(deps);
			}
			
			if (++cnt >= 1) {
				break;
			}
		}
	}
	
	

	public static void main(String[] args) {
		String xlsxFilePath =
				"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx";
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-trial");
		HashMap<Integer, AnnotatedSentence> annotatedSentences =
				new HashMap<Integer, AnnotatedSentence>();
		try {
			XSSFDataRetriever.readXSSFAnnotations(xlsxFilePath,
					trainCorpus, annotatedSentences);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		test(trainCorpus, annotatedSentences);
	}
}
