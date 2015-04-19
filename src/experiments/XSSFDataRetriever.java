package experiments;

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
import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.Proposition;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;	

public class XSSFDataRetriever {

	private static String xlsxFilePath =
		//	"odesk/r2_s100_new_with_samples copy.xlsx";
		//	"odesk/raw_annotation/odesk_r3_s100_breanna_fixed.xlsx";
		//	"odesk/raw_annotation/odesk_r3_s100_breanna.xlsx";
		//	"odesk/reviewed_annotation/r2_s100_new_with_samples_breanna_luheng_updated.xlsx";
		//	"odesk/training/r2_s100_new_with_samples donna_20150404.xlsx";
		//	"odesk/raw_annotation/odesk_r6_p1_s50_francine_fixed.xlsx";
		//	"odesk/training/odesk_r3_s100_katie.xlsx";
		//	"odesk/raw_annotation/odesk_r2_s90_donna_fixed.xlsx";
		//	"odesk/reviewed_annotation/odesk_r3_s100_john.xlsx";
			"odesk/raw_annotation/odesk_r15_p1_s50_sarah_fixed.xlsx";
		//	"odesk/raw_annotation/odesk_r6_p2_s50_tracy_fixed.xlsx";

	
	private static int getHeaderId(String header) {
		if (!header.contains("_")) {
			return -1;
		}
		return Integer.parseInt(header.substring(header.indexOf('_') + 1));
	}
	
	public static void readXSSFAnnotations(
			String filePath, SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences)
					throws IOException {
		readXSSFAnnotations(filePath, corpus, annotatedSentences, null);
	}
	
	public static void readXSSFAnnotation(
			String[] inputFiles, SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotations)
					throws FileNotFoundException, IOException {
		// Map sentence ids to a set of AnnotatedSentence
		assert (annotations != null);
		
		for (String inputFile : inputFiles) {
			XSSFWorkbook workbook = new XSSFWorkbook(
					new FileInputStream(new File(inputFile)));
		         
			int unitId = -1, sentId = -1, propHead = -1;
			SRLSentence sent = null;
			AnnotatedSentence currSent = null;
			ArrayList<QAPair> qaList = new ArrayList<QAPair>();
			
			int numSentsPerFile = 0;
			for (int sn = 0; sn < workbook.getNumberOfSheets(); sn++) {
				XSSFSheet sheet = workbook.getSheetAt(sn);
				for (int r = 0; r <= sheet.getLastRowNum(); r++) {
					XSSFRow row = sheet.getRow(r);
		        	if (row == null || row.getLastCellNum() == 0) {
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
		        			question[c-1] = row.getCell(c).getStringCellValue();        		
		        		}
		        	}
		        	QAPair qa = new QAPair(sent, propHead, question,
		        			"" /* answer */,
		        			null /* cf source */);
		        	qa.annotator = inputFile;
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
		        	}
		        }
//				System.out.println(sheet.getSheetName() + " ... " + annotations.size());
			 }			
			workbook.close();
			System.out.println(String.format("Read %d sentences from %s.",
					numSentsPerFile, inputFile));
		}
	}
	
	public static void readXSSFAnnotations(
			String filePath, SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences,
			int[] sheetIds)
					throws IOException {
		assert (annotatedSentences != null);
		
        XSSFWorkbook workbook =
        		new XSSFWorkbook(new FileInputStream(new File(filePath)));
        
        int unitId = -1, sentId = -1, propHead = -1, totalNumQAs = 0,
        	qaPerUnit = 0;
        boolean hasEmptyAnswer = false;
        String prevSheetName = "";
        SRLSentence sent = null;
        
        if (sheetIds == null) {
        	sheetIds = new int[workbook.getNumberOfSheets()];
        	for (int i = 0; i < sheetIds.length; i++) {
        		sheetIds[i] = i;
        	}
        }
        
        for (int sn : sheetIds) {
        	XSSFSheet sheet = workbook.getSheetAt(sn);    
	        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
	        	XSSFRow row = sheet.getRow(r);
	        	if (row == null || row.getLastCellNum() == 0) {
	        		continue;
	        	}
	        	String header = row.getCell(0).getStringCellValue();
	        	if (header.startsWith("UNIT")) {
	        		if (unitId > -1) {
	        			if (qaPerUnit == 0) {
	        				System.out.println(String.format(
	        						"Empty unit at:\t%s, UNIT_%05d",
	        						prevSheetName, unitId));
	        			}
	        			if (hasEmptyAnswer) {
	        				System.out.println(String.format(
	        						"Unanswered question at:\t%s, UNIT_%05d",
	        						prevSheetName, unitId));
	        			}
	        		}
	        		unitId = getHeaderId(header);
	        		qaPerUnit = 0;
	        		hasEmptyAnswer = false;
	        		prevSheetName = sheet.getSheetName();
	        	} else if (header.startsWith("SENT")) {
	        		sentId = getHeaderId(header);
	        		sent = corpus.getSentence(sentId);
	        		if (!annotatedSentences.containsKey(sentId)) {
	        			annotatedSentences.put(sentId, new AnnotatedSentence(sent));
	        		}
	        	} else if (header.startsWith("TRG")) {
	        		propHead = getHeaderId(header);
	        		annotatedSentences.get(sentId).addProposition(propHead);
	        	} 
	        	if (!header.startsWith("QA") ||
	        		row.getCell(1) == null ||
	        		row.getCell(1).toString().isEmpty()) {
	        		continue;
	        	}
	        	String[] question = new String[7];
	        	for (int c = 1; c <= 7; c++) {
	        		if (row.getCell(c) == null) {
	        			question[c-1] = "";
	        			continue;
	        		} else {
	        			question[c-1] = row.getCell(c).getStringCellValue();        		
	        		}
	        	}
	        	QAPair qa = new QAPair(sent, propHead, question,
	        			"" /* answer */, null /* cf source */);
	        	int ansPerQuestion = 0;
	        	for (int c = 9; c <= 13; c++) {
	        		if (row.getCell(c) == null) {
	        			continue;
	        		}
	        		String ans = row.getCell(c).toString();
	        		if (!ans.isEmpty()) {
	        			if (qa.addAnswer(ans)) {
	        				ansPerQuestion ++;
	        			} else {
	        				System.out.println("unaligned answer:\t" + ans);
	        			}
	        		}
	        	}
	        	if (row.getCell(14) != null) {
	        		qa.comment = row.getCell(14).getStringCellValue().trim();
	        	}
	        	if (ansPerQuestion == 0) {
	        		hasEmptyAnswer = true;
	        		continue;
	        	}
	        	if (!question[0].isEmpty() && !question[3].isEmpty()) {
	        		annotatedSentences.get(sentId).addQAPair(propHead, qa);
		        	totalNumQAs ++;
		        	qaPerUnit ++;
	        	}
	        }
        }
        workbook.close();
        
        if (qaPerUnit == 0) {
			System.out.println(String.format(
					"Empty unit at:\t%s, UNIT_%05d",
					prevSheetName, unitId));
		}
		if (hasEmptyAnswer) {
			System.out.println(String.format(
					"Unanswered question at:\t%s, UNIT_%05d",
					prevSheetName, unitId));
		}
       
        System.out.println(String.format("%d units read from %s, covering %d sentences.",
        		unitId, filePath, annotatedSentences.size()));
        System.out.println(String.format("Total number of QAs: %d", totalNumQAs));
	}
	
	public static void aggregateAnnotations(
			SRLCorpus corpus, HashMap<Integer, AnnotatedSentence> annotations) {
		
		System.out.println(String.format("Processing %d sentences.", annotations.size()));
		double avgAgreement = .0;
		int numMultiAnnotatedUnits = 0,
			numMultiAnnotatedSentences = 0,
			numTotalProposedQAs = 0,
			numTotalAgreedQAs = 0;
		
		for (int sid : annotations.keySet()) {
			AnnotatedSentence annotSent = annotations.get(sid);
			SRLSentence sent = (SRLSentence) annotSent.sentence;
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
			SRLCorpus baseCorpus,
			HashMap<Integer, AnnotatedSentence> annotations) throws IOException {
		// Get sentence ids and sort.
		int[] sentIds = getSortedKeys(annotations.keySet());
		
		// Output to text file.
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
		int numSentsWritten = 0;
		for (int sid : sentIds) {
			AnnotatedSentence annotSent = annotations.get(sid);
			SRLSentence sent = (SRLSentence) annotSent.sentence;
			
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
				writer.write(String.format("%d\t%s\t%d\n",
						propHead, prop, qaList.size()));
				for (QAPair qa : qaList) {
					writer.write(qa.getPaddedQuestionString() + "\t");
					for (int i = 0; i < qa.answers.size(); i++) {
						writer.write((i > 0 ? " ### " : "") + qa.answers.get(i).trim());
					}
					writer.write("\n");
				}
			}
			writer.write("\n");
			numSentsWritten ++;
		}
		writer.close();
		System.out.println(String.format(
				"Skipped %d empty sentences. Successfully wrote %d sentences to %s.",
				sentIds.length - numSentsWritten,
				numSentsWritten,
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
	
	public static void main(String[] args) {
		SRLCorpus trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		
		HashMap<Integer, AnnotatedSentence> annotations =
				new HashMap<Integer, AnnotatedSentence>();
				
		try {
			readXSSFAnnotations(
					xlsxFilePath,
					trainCorpus,
					annotations);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
		tester.computeSRLAccuracy(annotations.values(), trainCorpus);
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotations.values(), trainCorpus);
		
	//	debugOutput(trainCorpus, annotatedSentences);
	}
}
