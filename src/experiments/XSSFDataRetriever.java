package experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import annotation.SRLAnnotationValidator;
import data.AnnotatedSentence;
import data.QAPair;
import data.SRLCorpus;
import data.SRLSentence;

public class XSSFDataRetriever {

	private static String xlsxFilePath =
		//	"odesk/r2_s100_new_with_samples copy.xlsx";
		//	"odesk/raw_annotation/r2_s100_new_with_samples_breanna.xlsx";
			"odesk/reviewed_annotation/r2_s100_new_with_samples_breanna_luheng.xlsx";
	
	private static int getHeaderId(String header) {
		if (!header.contains("_")) {
			return -1;
		}
		return Integer.parseInt(header.substring(header.indexOf('_') + 1));
	}
	
	private static void readXSSFAnnotations(
			String filePath, SRLCorpus corpus,
			HashMap<Integer, AnnotatedSentence> annotatedSentences)
					throws IOException {
		assert (annotatedSentences != null);
		
        XSSFWorkbook workbook =
        		new XSSFWorkbook(new FileInputStream(new File(filePath)));
        
        int unitId = 0, sentId = 0, propHead = 0;
        SRLSentence sent = null;
        
        for (int sn = 1; sn < workbook.getNumberOfSheets(); sn++) {
        	XSSFSheet sheet = workbook.getSheetAt(sn);    
	        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
	        	XSSFRow row = sheet.getRow(r);
	        	if (row == null || row.getLastCellNum() == 0) {
	        		continue;
	        	}
	        	String header = row.getCell(0).getStringCellValue();
	        	if (header.startsWith("UNIT")) {
	        		unitId = getHeaderId(header);
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
	        	for (int c = 9; c <= 13; c++) {
	        		if (row.getCell(c) == null) {
	        			continue;
	        		}
	        		String ans = row.getCell(c).toString();
	        		/*
	        		try {
	        			ans = row.getCell(c).getStringCellValue().trim();
	        		} catch (Exception e) {
	        			System.out.println(row.getCell(c).toString());
	        		}*/
	        		if (!ans.isEmpty()) {
	        			qa.addAnswer(ans);
	        		}
	        	}
	        	qa.comment = row.getCell(14).getStringCellValue().trim();
	        	annotatedSentences.get(sentId).addQAPair(propHead, qa);
	        }
        }
        workbook.close();
        System.out.println(String.format("%d units read from %s, covering %d sentences.",
        		unitId, filePath, annotatedSentences.size()));
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
		tester.ignoreLabels = true;
		tester.computeSRLAccuracy(annotatedSentences.values(), trainCorpus);
	}
}
