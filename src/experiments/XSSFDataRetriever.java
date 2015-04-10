package experiments;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
		//	"odesk/raw_annotation/odesk_r4_s100_ellen_fixed.xlsx";
		//	"odesk/training/odesk_r3_s100_katie.xlsx";
		//	"odesk/training/FrancinePoh_R6.xlsx";
		//	"odesk/raw_annotation/odesk_r2_s90_donna.xlsx";
			"odesk/reviewed_annotation/r2_s100_new_with_samples_dawn.xlsx";
	
	private static int[] finishedSheetIds = { 1, 2 };
	
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
	        	/*		} else if (qaPerUnit == 1) {
	        				System.out.println(String.format(
	        						"Please double check:\t%s, UNIT_%05d",
	        						prevSheetName, unitId)); */
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
	        		sent = (SRLSentence) corpus.sentences.get(sentId);
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
	        	//System.out.println(unitId + ", " + row.getCell(0).toString() + ", " + row.getLastCellNum());
	        	String[] question = new String[7];
	        	for (int c = 1; c <= 7; c++) {
	        		question[c-1] = row.getCell(c).getStringCellValue();        		
	        	}
	        	QAPair qa = new QAPair(sent, propHead, question,
	        			"" /* answer */, null /* annotation source */);
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
	        	qa.comment = row.getCell(14).getStringCellValue().trim();
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
		} /*else if (qaPerUnit == 1) {
			System.out.println(String.format(
					"Please double check:\t%s, UNIT_%05d",
					prevSheetName, unitId)); 
		} */
		if (hasEmptyAnswer) {
			System.out.println(String.format(
					"Unanswered question at:\t%s, UNIT_%05d",
					prevSheetName, unitId));
		}
       
        System.out.println(String.format("%d units read from %s, covering %d sentences.",
        		unitId, filePath, annotatedSentences.size()));
        System.out.println(String.format("Total number of QAs: %d", totalNumQAs));
	}
	
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
			SRLSentence sentence = annotSent.sentence;
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
		
		HashMap<Integer, AnnotatedSentence> annotatedSentences =
				new HashMap<Integer, AnnotatedSentence>();
				
		try {
			readXSSFAnnotations(xlsxFilePath, trainCorpus, annotatedSentences);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// TODO align annotation
		
		SRLAnnotationValidator tester = new SRLAnnotationValidator();
	//	tester.coreArgsOnly = true;
		tester.computeSRLAccuracy(annotatedSentences.values(), trainCorpus);
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences.values(), trainCorpus);
		
	//	debugOutput(trainCorpus, annotatedSentences);
	}
}
