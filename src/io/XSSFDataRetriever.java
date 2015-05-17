package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import util.LatticeUtils;
import annotation.QuestionEncoder;
import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Corpus;
import data.Proposition;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;
import data.Sentence;

public class XSSFDataRetriever {
	
	private static int getHeaderId(String header) {
		if (!header.contains("_")) {
			return -1;
		}
		return Integer.parseInt(header.substring(header.indexOf('_') + 1));
	}
	
	public static void readXSSFAnnotation(
			String[] inputFiles,
			Corpus corpus,
			HashMap<Integer, AnnotatedSentence> annotations)
					throws FileNotFoundException, IOException {
		// Map sentence ids to a set of AnnotatedSentence
		assert (annotations != null);
		HashMap<Integer, Integer> sentIdMap = new HashMap<Integer, Integer>();
		
		for (String inputFile : inputFiles) {
			XSSFWorkbook workbook = new XSSFWorkbook(
					new FileInputStream(new File(inputFile)));
		         
			int unitId = -1, sentId = -1, propHead = -1;
			Sentence sent = null;
			AnnotatedSentence currSent = null;
			ArrayList<QAPair> qaList = new ArrayList<QAPair>();
			
			int numSentsPerFile = 0, numQAsPerFile = 0;
			for (int sn = 0; sn < workbook.getNumberOfSheets(); sn++) {
				XSSFSheet sheet = workbook.getSheetAt(sn);
				for (int r = 0; r <= sheet.getLastRowNum(); r++) {
					XSSFRow row = sheet.getRow(r);
		        	if (row == null || row.getLastCellNum() == 0 ||
		        		row.getCell(0) == null) {
		        		continue;
		        	}
		        	String header = row.getCell(0).getStringCellValue();
		        	if (header.startsWith("UNIT")) {
		        		if (unitId > -1 && !qaList.isEmpty()) {
		        			// Process previous unit.
		        			currSent.addProposition(propHead);
		        			for (QAPair qa : qaList) {
		        				currSent.addQAPair(propHead, qa);
		        			}
		        			qaList.clear();
		        		}
		        		unitId = getHeaderId(header);
		        	} else if (header.startsWith("SENT")) {
		        		if (sentId != getHeaderId(header)) {
		        			// Encountering a new sentence.
			        		sentId = getHeaderId(header);
			        		// A hacky way to process Wikipedia data.
			        		if (corpus.sentences.size() <= sentId) {
			        			if (sentIdMap.containsKey(sentId)) {
			        				sentId = sentIdMap.get(sentId);
			        			} else {
			        				String sentStr = row.getCell(1).toString().trim();
			        				sent = corpus.addNewSentence(sentStr);
				        			sentIdMap.put(sentId, sent.sentenceID);
				        			sentId = sent.sentenceID;
			        			}
			        		}
			        		sent = corpus.getSentence(sentId);
			        		if (!annotations.containsKey(sentId)) {
			        			annotations.put(sentId, new AnnotatedSentence(sent));
			        			numSentsPerFile ++;
			        		}
			        		currSent = annotations.get(sentId);
		        		}
		        	} else if (header.startsWith("TRG")) {
		        		propHead = getHeaderId(header);
		        	} 
		        	if (!header.startsWith("QA") || row.getCell(1) == null ||
		        			row.getCell(1).toString().isEmpty()) {
		        		continue;
		        	}
		        	String[] question = new String[7];
		        	for (int c = 1; c <= 7; c++) {
		        		if (row.getCell(c) == null) {
		        			question[c-1] = "";
		        			continue;
		        		} else {
		        			question[c-1] = row.getCell(c).getStringCellValue().trim();     		
		        		}
		        	}
		        	// Normalizing question:
		        	//   If ph3 in {someone, something}, and ph2=null, pp=null,
		        	//   ph3 is moved to ph2
		        	QuestionEncoder.normalize(question);
		        	QAPair qa = new QAPair(
		        			sent, propHead, question,
		        			"" /* answer */,
		        			inputFile /* annotator source */);
		        	for (int c = 9; c <= 13; c++) {
		        		if (row.getCell(c) == null) {
		        			continue;
		        		}
		        		String ans = row.getCell(c).toString();
		        		if (!ans.isEmpty()) {
		        			qa.addAnswer(ans);
		        		}
		        	}
		        	if (row.getCell(14) != null) {
		        		qa.comment = row.getCell(14).getStringCellValue().trim();
		        	}
		        	if (!question[0].isEmpty() && !question[3].isEmpty() &&
		        			!qa.getAnswerString().isEmpty()) {
		        		qaList.add(qa);
		        		numQAsPerFile ++;
		        	}
		        }
//				System.out.println(sheet.getSheetName() + " ... " + annotations.size());
			 }			
			workbook.close();
			System.out.println(String.format(
					"Read %d sentences and %d QAs from %s.",
						numSentsPerFile, numQAsPerFile, inputFile));
		}
	}
	
	
	public static void aggregateAnnotations(
			Corpus corpus, HashMap<Integer, AnnotatedSentence> annotations) {
		
		System.out.println(String.format("Processing %d sentences.", annotations.size()));
		double avgAgreement = .0;
		int numMultiAnnotatedUnits = 0,
			numMultiAnnotatedSentences = 0,
			numTotalProposedQAs = 0,
			numTotalAgreedQAs = 0;
		
		for (int sid : annotations.keySet()) {
			AnnotatedSentence annotSent = annotations.get(sid);
			Sentence sent = (Sentence) annotSent.sentence;
			if (annotSent.annotators.size() == 1) {
				continue;
			}	
			numMultiAnnotatedSentences ++;
			
			for (int propHead : annotSent.qaLists.keySet()) {
				HashMap<String, int[]> qaMap = new HashMap<String, int[]>();
				HashMap<String, HashSet<String>> questionMap =
						new HashMap<String, HashSet<String>>();
				ArrayList<QAPair> newQAList = new ArrayList<QAPair>();
				
				int numProposedQAs = 0, numAgreedQAs = 0;
				for (QAPair qa : annotSent.qaLists.get(propHead)) {
					String qlabel = qa.getQuestionLabel();
					String qstr = qa.getQuestionString().trim();
					if (!qaMap.containsKey(qlabel)) {
						int[] flags = new int[sent.length];
						Arrays.fill(flags, 0);
						qaMap.put(qlabel, flags);
						questionMap.put(qlabel, new HashSet<String>());
					}
					int[] flags = qaMap.get(qlabel);
					for (int i = 0; i < sent.length; i++) {
						flags[i] += (qa.answerFlags[i] > 0 ? 1 : 0);
					}
					questionMap.get(qlabel).add(qstr);
				}
				numProposedQAs = qaMap.size();
				for (String qlabel : qaMap.keySet()) {
					int agreement = 0;
					for (int agr : qaMap.get(qlabel)) {
						agreement = Math.max(agreement, agr);
					}
					if (agreement > 1) {
						numAgreedQAs ++;
						/*
						System.out.println(qlabel);
						for (String qstr : questionMap.get(qlabel)) {
							System.out.println(qstr);
						}
						System.out.println();
						*/
						String anyQuestion = questionMap.get(qlabel).iterator().next();
						QAPair newQA = new QAPair(sent, propHead, anyQuestion,
								"" /* answer */, null /* cf source */);
						newQA.addAnswer(qaMap.get(qlabel));
						newQAList.add(newQA);
					}
				}
				if (numProposedQAs > 0) {
					numMultiAnnotatedUnits ++;
					avgAgreement += 1.0 * numAgreedQAs / numProposedQAs;
					numTotalProposedQAs += numProposedQAs;
					numTotalAgreedQAs += numAgreedQAs;
					annotSent.qaLists.put(propHead, newQAList);
				}
			}
		}
		System.out.println(String.format(
				"Number of multi-annotated sentences: %d\n" +
				"Number of multi-annotated units: %d\n" +
				"Averaged agreement ratio: %.3f\n",
				numMultiAnnotatedSentences,
				numMultiAnnotatedUnits,
				avgAgreement / numMultiAnnotatedUnits));
		System.out.println(String.format(
				"Number of proposed QAs: %d\n" +
				"Number of agreed QAs: %d\n",
				numTotalProposedQAs,
				numTotalAgreedQAs));
	}
	
	private static int[] getSortedKeys(Collection<Integer> keys) {
		int[] sortedKeys = new int[keys.size()];
		int sn = 0;
		for (int k : keys) {
			sortedKeys[sn++] = k;
		}
		Arrays.sort(sortedKeys);
		return sortedKeys;
 	}
	
	public static void outputAnnotations(String outputPath,
			Corpus baseCorpus,
			HashMap<Integer, AnnotatedSentence> annotations) throws IOException {
		// Get sentence ids and sort.
		int[] sentIds = getSortedKeys(annotations.keySet());
		
		// Output to text file.
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
		int numSentsWritten = 0,
			numPropsWritten = 0,
			numQAsWritten = 0;
		for (int sid : sentIds) {
			AnnotatedSentence annotSent = annotations.get(sid);
			Sentence sent = annotSent.sentence;
			
			// Filter empty sentences.
			int[] propIds = getSortedKeys(annotSent.qaLists.keySet());
			int numProps = 0;
			for (int propHead : propIds) {
				if (annotSent.qaLists.get(propHead).size() > 0) {
					numProps ++;
				}
			}
			if (numProps == 0) {
				continue;
			}
			// Write sentence info.
			writer.write(String.format("%s_%d\t%d\n",
					baseCorpus.corpusName, sid, numProps));
			writer.write(sent.getTokensString() + "\n");
			for (int propHead : propIds) {
				String prop = sent.getTokenString(propHead).toLowerCase();
				ArrayList<QAPair> qaList = annotSent.qaLists.get(propHead);
				if (qaList.size() == 0) {
					continue;
				}
				writer.write(String.format("%d\t%s\t%d\n",
						propHead, prop, qaList.size()));
				for (QAPair qa : qaList) {
					writer.write(qa.getPaddedQuestionString() + "\t");
					for (int i = 0; i < qa.answers.size(); i++) {
						writer.write((i > 0 ? " ### " : "") + qa.answers.get(i).trim());
					}
					writer.write("\n");
				}
				numQAsWritten += qaList.size();
			}
			writer.write("\n");
			numSentsWritten ++;
			numPropsWritten += numProps;
		}
		writer.close();
		System.out.println(String.format(
				"Skipped %d empty sentences. " +
				"Successfully wrote %d sentences, %d proposition and %d QAs to %s.",
				sentIds.length - numSentsWritten,
				numSentsWritten,
				numPropsWritten,
				numQAsWritten,
				outputPath));
	}
	
	@SuppressWarnings("unused")
	private static void debugOutput(SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences) {
		SRLAnnotationValidator validator = new SRLAnnotationValidator();
		validator.ignoreLabels = true;
		int sentCount = 0;
		/*
		try {
			System.setOut(new PrintStream(new BufferedOutputStream(
					new FileOutputStream("odesk/debug/odesk_debug_r3_breanna.xls"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/
		for (AnnotatedSentence annotSent : annotatedSentences.values()) {
			sentCount ++;
			SRLSentence sentence = (SRLSentence) annotSent.sentence;
			String[][] gold = validator.getGoldSRL(sentence);
			int[][] covered = new int[gold.length][];
			for (int i = 0; i < gold.length; i++) {
				covered[i] = new int[gold[i].length];
			}
			LatticeUtils.fill(covered, 0);
			int sentLength = sentence.length;
			for (Proposition prop : sentence.propositions) {
				int propId = prop.propID;
				if (!annotSent.qaLists.containsKey(propId)) {
					continue;
				}
				//System.out.println(annotSent.sentence.getTokenString(propId));
				// Compute Agreement.
				HashMap<String, int[]> qaCount = new HashMap<String, int[]>();
				for (QAPair qa : annotSent.qaLists.get(propId)) {
					String qlabel = qa.questionLabel;
					if (!qaCount.containsKey(qlabel)) {
						qaCount.put(qlabel, new int[sentLength]);
						Arrays.fill(qaCount.get(qlabel), 0);
					}
					int[] flags = qaCount.get(qlabel);
					for (int i = 0; i < sentLength; i++) {
						flags[i] += (qa.answerFlags[i] > 0 ? 1 : 0);
						if (!gold[propId + 1][i + 1].isEmpty() &&
							validator.matchedGold(i, qa, sentence)) {
							covered[propId + 1][i + 1] = 1;
						}
					}
				}
				System.out.println(
						sentence.sentenceID + "\t" +
						sentence.getTokensString());
				System.out.println(
						sentence.getTokenString(propId) + "\t" +
						corpus.propDict.getString(prop.propType));
				for (int i = 0; i < prop.argIDs.size(); i++) {
					int argTypeId = prop.argTypes.get(i),
						argId = prop.argIDs.get(i);
					String argType = corpus.argModDict.getString(argTypeId);
					if (argType.equals("AM-NEG") ||
						argType.equals("AM-MOD") ||
						argType.equals("AM-DIS") ||
						argType.startsWith("R-")) {
						continue;
					}
					System.out.println(
							" \t" + argType + "\t" +
							" \t" + sentence.getTokenString(argId) + "\t" +
							(covered[propId + 1][argId + 1] > 0 ? " " : "NC"));
				}
				for (QAPair qa : annotSent.qaLists.get(propId)) {
					String qlabel = qa.getQuestionLabel();
					boolean matched = false;
					int[] flags = qaCount.get(qlabel);
					for (int i = 0; i < sentLength; i++) {
						if (!gold[propId + 1][i + 1].isEmpty() &&
							validator.matchedGold(i, qa, sentence)) {
							matched = true;
						}
					}
					System.out.println(
				//			workerId + "\t" +
							qa.questionLabel + "\t" +
							qa.getQuestionString() + "\t" +
							qa.getAnswerString() + "\t" + 
//							(agreed ? " " : "NA") + "\t" +
							(matched ? " " : "NG") + "\t");
				}
				System.out.println();
			}
		}
	}
}
