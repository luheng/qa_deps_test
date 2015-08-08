package baselines;

import com.sun.tools.javac.comp.Annotate;
import io.XSSFDataRetriever;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.CountDictionary;
import data.QAPair;
import data.SRLCorpus;
import evaluation.Results;
import util.StrUtils;

public class InterAnnotatorAgreementExperiment2 {

	private static String dataPath1 =
		"/Users/luheng/versioned/qa_deps_test/odesk_agreement/annotated";

	private static String dataPath2 =
		"/Users/luheng/versioned/qa_deps_test/odesk_agreement/camera_ready/annotated";
	
	private static String[] inputFiles1 = {
		"odesk_pb_r3_s30_breanna.xlsx",
		"odesk_pb_r3_s30_ellen.xlsx",
		"odesk_pb_r3_s30_john.xlsx",
		"odesk_pb_r3_s30_katie.xlsx",
		"odesk_pb_r3_s30_tracy.xlsx"
	};

	private static String[] inputFiles2 = {
			"odesk_r3_s70_breanna.xlsx",
			"odesk_r3_s70_donna.xlsx",
			"odesk_r3_s70_john.xlsx",
			"odesk_r3_s70_katie.xlsx",
			"odesk_r3_s70_tracy.xlsx"
	};

	private static String[] inputFiles3 = {
		"odesk_wiki1_r014_s30_breanna.xlsx",
		"odesk_wiki1_r014_s30_ellen.xlsx",
		"odesk_wiki1_r014_s30_john.xlsx",
		"odesk_wiki1_r014_s30_katie.xlsx",
		"odesk_wiki1_r014_s30_tracy.xlsx"
	};


	private static int numAnnotators = 5;

	private static void readData(
			String[] inputFiles,
			Corpus baseCorpus,
			HashMap<Integer, AnnotatedSentence> annotations) {
		try {
			XSSFDataRetriever.readXSSFAnnotation(
					inputFiles,
					baseCorpus,
					annotations);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(String.format("Read %d sentences.", annotations.size()));

        /******** Remap annotators names **********/
        CountDictionary annotators = new CountDictionary();
        String[] ant = getAnnotators(annotations);
        for (String a : ant) {
            annotators.addString(a);
        }
        annotators.prettyPrint();
        for (AnnotatedSentence sent : annotations.values()) {
            for (int propHead : sent.qaLists.keySet()) {
                for (QAPair qa : sent.qaLists.get(propHead)) {
                    String annot = getShortAnnotatorName((String) qa.annotators.get(0));
                    int annotId = annotators.lookupString(annot);
                  //  System.out.println(annotId + "\t" + annot);
                    qa.annotators.set(0, String.format("Annotator%02d", annotId));
                }
            }
        }
        annotators = new CountDictionary();
        ant = getAnnotators(annotations);
        for (String a : ant) {
            annotators.addString(a);
        }
        annotators.prettyPrint();
	}

	private static String[] getAnnotators(HashMap<Integer, AnnotatedSentence> annotations) {
		HashSet<String> annotators = new HashSet<>();
		for (AnnotatedSentence sent : annotations.values()) {
			for (int propHead : sent.qaLists.keySet()) {
				for (QAPair qa : sent.qaLists.get(propHead)) {
					String ant = (String) qa.annotators.get(0);
					annotators.add(getShortAnnotatorName(ant));
				}
			}
		}
		return annotators.toArray(new String[annotators.size()]);
	}

	private static String getShortAnnotatorName(String annotator) {
        String[] tmp = annotator.split("_");
        return tmp[tmp.length - 1].split("\\.")[0];
    }
	
	private static String getQuestionLabel(QAPair qa) {
		String qw = qa.questionWords[0].toUpperCase();
		if (qw.equals("WHO") || qw.equals("WHAT")) {
			qw = "WHOWHAT";
		}
		return qw;
	}
	
	private static boolean equals(QAPair qa1, QAPair qa2) {
		// String l1 = qa1.questionLabel.split("=")[0],
		//	      l2 = qa2.questionLabel.split("=")[0];
		String l1 = getQuestionLabel(qa1), l2 = getQuestionLabel(qa1);
		int answerOverlap = 0;
		for (int i = 0; i < qa1.answerFlags.length; i++) {
			if (qa1.answerFlags[i] > 0 && qa2.answerFlags[i] > 0) {
				answerOverlap ++;
			}
		}
		boolean qwEq = l1.split("_")[0].equals(l2.split("_")[0]);
		return qwEq && answerOverlap > 0;
	}
	
	private static HashMap<Integer, HashMap<Integer, ArrayList<QAPair>>>
			computeAgreement(
                    HashMap<Integer, AnnotatedSentence> annotations,
                    CountDictionary annotators,
					int[] subset) {
		HashMap<Integer, HashMap<Integer, ArrayList<QAPair>>> ret = new HashMap<>();
		for (AnnotatedSentence annotSent : annotations.values()) {
			// System.out.println(annotSent.sentence.getTokensString());
			int sentId = annotSent.sentence.sentenceID;
			ret.put(sentId, new HashMap<Integer, ArrayList<QAPair>>());
			for (int propHead : annotSent.qaLists.keySet()) {
				// System.out.println(annotSent.sentence.getTokenString(propHead));
				ret.get(sentId).put(propHead, new ArrayList<>());
				ArrayList<QAPair> agreedQAs = ret.get(sentId).get(propHead);
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String annotator = getShortAnnotatorName((String) qa.annotators.get(0));
                    int antId = annotators.lookupString(annotator);
					if (subset[antId] == 0) {
						continue;
					}
					boolean agreed = false;
					for (QAPair qa2 : agreedQAs) {
						if (qa2.annotators.contains(annotator)) {
							continue;
						}
						if (equals(qa, qa2)) {
							for (int i = 0; i < qa2.answerFlags.length; i++) {
								qa2.answerFlags[i] = Math.min(qa.answerFlags[i], qa2.answerFlags[i]);
								/* if (qa.answerFlags[i] > 0) {
									qa2.answerFlags[i] ++;
								} */
							}
							qa2.annotators.add(annotator);
							agreed = true;
							break;
						}
					}
					if (!agreed) {						
						QAPair qa2 = new QAPair(
								qa.sentence, propHead, qa.questionWords,
			        			"" /* answer */,
								annotator);
						for (int i = 0; i < qa2.answerFlags.length; i++) {
							qa2.answerFlags[i] = Math.min(1, qa.answerFlags[i]);
						}
						agreedQAs.add(qa2);
					}
				}
				/* for (QAPair qa : agreedQAs) {
					System.out.println(
							qa.annotators.size() + "\t" +
							qa.questionLabel + "\t" + 
							qa.getQuestionString() + "\t" +
							qa.getAnswerString() + "\t" +
							StrUtils.join(",", qa.annotators));					
				} */
			}
		}
		return ret;
	}

	private static void computeAgreement(HashMap<Integer, AnnotatedSentence> annotations) {
        CountDictionary annotators = new CountDictionary();
        String[] ant = getAnnotators(annotations);
        for (String a : ant) {
            annotators.addString(a);
        }
        annotators.prettyPrint();

		ArrayList<Results> r1 = new ArrayList<>();
		ArrayList<Results> r2 = new ArrayList<>();
		for (int i = 0; i < numAnnotators; i++) {
			r1.add(new Results());
			r2.add(new Results());
		}

		HashMap<Integer, HashMap<Integer, ArrayList<QAPair>>> validatedQAs =
				computeAgreement(annotations, annotators, new int[] {1, 1, 1, 1, 1});
		for (AnnotatedSentence sent : annotations.values()) {
			int sentId = sent.sentence.sentenceID;
			for (int propHead : sent.qaLists.keySet()) {
				ArrayList<QAPair> qaList = new ArrayList<>();
				for (QAPair qa : validatedQAs.get(sentId).get(propHead)) {
					if (qa.annotators.size() > 1) {
						qaList.add(qa);
					}
				}
				validatedQAs.get(sentId).put(propHead, qaList);
			}
		}

		for (int i = 1; i < 32; i++) {
			int[] subset = new int[numAnnotators];
			int r = i, na = 0;
			for (int k = 0; k < 5; k++) {
				subset[k] = r % 2;
				na += r % 2;
				r /= 2;
			}
			HashMap<Integer, HashMap<Integer, ArrayList<QAPair>>> ret =
                    computeAgreement(annotations, annotators, subset);
			int numQAs = 0, numProps = 0, numCorrectQAs = 0;
			for (AnnotatedSentence sent : annotations.values()) {
				int sentId = sent.sentence.sentenceID;
				for (int propHead : sent.qaLists.keySet()) {
					ArrayList<QAPair> agreedQAs = ret.get(sentId).get(propHead);
					for (QAPair qa : agreedQAs) {
						boolean correct = false;
						for (QAPair qa2 : validatedQAs.get(sentId).get(propHead)) {
							if (equals(qa, qa2)) {
								correct = true;
								break;
							}
						}
						if (correct) {
							numCorrectQAs ++;
						}
					}
					numQAs += agreedQAs.size();
					if (numQAs > 0) {
						++ numProps;
					}
                }
            }
            double avgQAs = 1.0 * numQAs / numProps;
			double avgCorrectQAs = 1.0 * numCorrectQAs / numProps;
			r1.get(na - 1).add(avgQAs);
			r2.get(na - 1).add(avgCorrectQAs);
			//System.out.println(StrUtils.intArrayToString(" ", subset) + ", " + na);
			//System.out.println(avgQAs);
			//System.out.println(avgCorrectQAs);
		}
		for (int i = 0; i < numAnnotators; i++) {
			System.out.println(
                    (i + 1) + "\t" +
                    r1.get(i).average() + "\t" + r1.get(i).std() + "\t" +
					r2.get(i).average() + "\t" + r2.get(i).std());
		}
	}

	public static void main(String[] args) {
		HashMap<Integer, AnnotatedSentence> annotations1 = new HashMap<>(),
                                            annotations2 = new HashMap<>();
		Corpus corpus = new Corpus("corpus");
		/********* Read first 30 sentences **********/
        String[] files = new String[numAnnotators];
		for (int i = 0; i < numAnnotators; i++) {
			files[i] = dataPath1 + "/" + inputFiles1[i];
		}
		readData(files, corpus, annotations1);
        
        /********* Read new data ... ************/
		for (int i = 0; i < numAnnotators; i++) {
			files[i] = dataPath2 + "/" + inputFiles2[i];
		}
		readData(files, corpus, annotations2);

        /****** combine 2 sets *********/
        annotations1.putAll(annotations2);

		computeAgreement(annotations1);
		//computeAgreement(annotations2);
	}
}
