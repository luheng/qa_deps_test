package experiments;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import annotation.AnnotationStage;
import annotation.AuxiliaryVerbIdentifier;
import annotation.CrowdFlowerQAResult;
import annotation.CrowdFlowerResult;
import annotation.CrowdFlowerStage2Result;
import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotPrepositions;
import annotation.QASlotQuestionWords;
import data.DepSentence;
import data.Proposition;
import data.SRLCorpus;
import data.SRLSentence;
import data.VerbInflectionDictionary;

public class CrowdFlowerQADataPreparation {
	
	private static SRLCorpus trainCorpus = null;
	private static final int maxNumSentences = 100;
	private static final int maxSentenceID = 5000;
	
	private static final int randomSeed = 12345;
	
	private static ArrayList<SRLSentence> sentences = null;
	private static ArrayList<ArrayList<Proposition>> propositions = null;
	
	private static VerbInflectionDictionary inflDict = null;
	private static AuxiliaryVerbIdentifier auxVerbID = null;
	
	private static String[] previousFileNames = new String[] {
			"crowdflower/CF_QA_firstround_s100.csv"
		};
	private static String outputFileName =
			"crowdflower/CF_QA_r2_s100_v2.csv";

	private static String[] kHeader = {"sent_id", "sentence", "orig_sent",
		"prop_id", "prop_head", "prop_start", "prop_end", "proposition",
		"trg_options", "pp_options"};
	
	private static String[] kAnnotationHeader = {"Annotation", "WH", "AUX",
		"PH1", "TRG", "PH2", "PP", "PH3", "?", "A1", "A2", "A3", "A4", "A5",
		"Review" };
	
	private static int maxNumQAs = 8;
	
	private static String getPartiallyHighlightedSentence(SRLSentence sentence,
			Proposition prop) {
		String sentStr = "";
		int s1 = prop.span[0], s2 = prop.span[1];
		for (int i = 0; i < sentence.length; i++) {
			if (i > 0) {
				sentStr += " ";
			}
			if (i == s1) {
				sentStr += "<mark>";
			}
			sentStr += sentence.getTokenString(i);
			if (i == s2 - 1) {
				sentStr += "</mark>";
			}
		}
		return sentStr;
	}
	
	private static ArrayList<String> getTrgOptions(DepSentence sent,
			int propHeadId) {
		String propHead = sent.getTokenString(propHeadId);
		ArrayList<Integer> inflIds =
				inflDict.inflMap.get(propHead.toLowerCase());
		
		if (inflIds == null) {
			//System.out.println("!!! Error:\t" + sent.getTokensString() + "\n" + propHead + " not found");
			System.out.println(propHead);
			//System.out.println(sent.getPostagsString());
			return null;
		}
		
		int bestId = -1, bestCount = -1;
		for (int i = 0; i < inflIds.size(); i++) {
			int count = inflDict.inflCount[inflIds.get(i)];
			if (count > bestCount) {
				bestId = inflIds.get(i);
				bestCount = count;
			}
		}
		// Generate list for dropdown.

		HashSet<String> opSet= new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		String[] inflections = inflDict.inflections.get(bestId);
		boolean usePresentParticiple = (propHead.toLowerCase().endsWith("ing"));
				
		for (int i = 0; i < inflections.length; i++) {
			if (i != 2) {
				opSet.add(inflections[i]);
			}
		}
		opSet.add("be " + inflections[4]);
 		opSet.add("been " + inflections[4]);
		opSet.add("have " + inflections[4]);
		opSet.add("have been " + inflections[4]);

		if (usePresentParticiple) {
			opSet.add("being " + inflections[4]);
			opSet.add(inflections[2]);
			opSet.add("be " + inflections[2]);
			opSet.add("been " + inflections[2]);
			opSet.add("have been " + inflections[2]);
		}
		for (String op : opSet) {
			options.add(op);
		}
		
		Collections.sort(options);
		return options;
	}
	
	private static ArrayList<String> getPPOptions(DepSentence sent) {
		HashSet<String> opSet = new HashSet<String>();
		ArrayList<String> options = new ArrayList<String>();
		for (int i = 0; i < sent.length; i++) {
			String tok = sent.getTokenString(i).toLowerCase();
			if (QASlotPrepositions.ppSet.contains(tok)) {
				opSet.add(tok);
				if (i < sent.length - 1) {
					String tok2 = sent.getTokenString(i + 1).toLowerCase();
					if (QASlotPrepositions.ppSet.contains(tok2)) {
						opSet.add(tok + " " + tok2);
						System.out.println(sent.getTokensString());
						System.out.println(tok + " " + tok2);
					}
				}
			}
		}
		for (String pp : QASlotPrepositions.mostFrequentPPs) {
			opSet.add(pp);
		}
		for (String op : opSet) {
			options.add(op);
		}
		Collections.sort(options);
		options.add(0, " ");
		return options;
	}
	
	private static String getCMLOptions(ArrayList<String> options) {
		String result = "";
		for (String option : options) {
			if (!result.isEmpty()) {
				result += "#";
			}
			result += option;
		}
		return result;
	}
	
	// Output the following fields:
	// "sent_id", "sentence", "orig_sent", "prop_id", "prop_head", "prop_start", "prop_end", "proposition", "trg_options"
	private static void outputUnits() throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		CSVPrinter csvWriter = new CSVPrinter(fileWriter, CSVFormat.EXCEL
				.withRecordSeparator("\n"));
		csvWriter.printRecord((Object[]) kHeader);
		for (int i = 0; i < maxNumSentences; i++) {
			SRLSentence sent = sentences.get(i);
			ArrayList<Proposition> props = propositions.get(i);
			if (props.size() == 0) {
				continue;
			}
			ArrayList<String> ppOptions = getPPOptions(sent);
			for (int j = 0; j < props.size(); j++) {
				Proposition prop = props.get(j);
				ArrayList<String> trgOptions = getTrgOptions(sent,
						prop.span[1] - 1);
				if (trgOptions == null) {
					continue;
				}
				ArrayList<String> row = new ArrayList<String>();
				row.add(String.valueOf(sent.sentenceID));
				row.add(getPartiallyHighlightedSentence(sent, prop));
				row.add(sent.getTokensString());
				row.add(String.valueOf(j));
				row.add(String.valueOf(prop.span[1] - 1));
				row.add(String.valueOf(prop.span[0]));
				row.add(String.valueOf(prop.span[1]));
				row.add(sent.getTokenString(prop.span));
				row.add(getCMLOptions(trgOptions));
				row.add(getCMLOptions(ppOptions));
				csvWriter.printRecord(row);
			}
		}
		csvWriter.close();
	}
	
	private static void outputXlsx() throws IOException {
		//Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
         
        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet("Annotation Batch 1");
        
        // Set editablity
        // sheet.protectSheet("password");
        CellStyle editable = workbook.createCellStyle();
        editable.setLocked(false);
        
        // DV helper
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
        
        int unitCounter = 0,
        	rowCounter = 0;
        
        CellRangeAddressList whCells = new CellRangeAddressList(),
        					 auxCells = new CellRangeAddressList(),
        					 ph1Cells = new CellRangeAddressList(),
        					 ph2Cells = new CellRangeAddressList(),
        					 ph3Cells = new CellRangeAddressList();
        
        for (int i = 0; i < maxNumSentences; i++) {
			SRLSentence sent = sentences.get(i);
			ArrayList<Proposition> props = propositions.get(i);
			if (props.size() == 0) {
				continue;
			}
			
			ArrayList<String> ppOptions = getPPOptions(sent);
			XSSFDataValidationConstraint ppConstraint =
				(XSSFDataValidationConstraint)
					dvHelper.createExplicitListConstraint(
						ppOptions.toArray(new String[ppOptions.size()]));

			
			for (int j = 0; j < props.size(); j++) {
				Proposition prop = props.get(j);
				ArrayList<String> trgOptions = getTrgOptions(sent,
						prop.span[1] - 1);
				if (trgOptions == null) {
					continue;
				}
				XSSFDataValidationConstraint trgConstraint =
					(XSSFDataValidationConstraint)
						dvHelper.createExplicitListConstraint(
							trgOptions.toArray(new String[trgOptions.size()]));
				
				// Write unit id and sentence ID
				Row row = sheet.createRow(rowCounter++);
				row.createCell(0).setCellValue("Unit ID");
				row.createCell(1).setCellValue(unitCounter++);
				
				// Write sentence
				row = sheet.createRow(rowCounter++);
				row.createCell(0).setCellValue("Sentence ID");
				row.createCell(1).setCellValue(sent.sentenceID);
				row.createCell(2).setCellValue("Sentence");
				// TODO: add highlight to prop
				row.createCell(3).setCellValue(sent.getTokensString());
				sheet.addMergedRegion(new CellRangeAddress(
			            row.getRowNum(), row.getRowNum(), 3, 50));
				
				// Write target word
				row = sheet.createRow(rowCounter++);
				row.createCell(0).setCellValue("Target ID");
				row.createCell(1).setCellValue(prop.span[1] - 1);
				row.createCell(2).setCellValue("Target");
				row.createCell(3).setCellValue(sent.getTokenString(prop.span));
				
				// Write annotation header
				row = sheet.createRow(rowCounter++);
				for (int c = 0; c < kAnnotationHeader.length; c++) {
					row.createCell(c).setCellValue(kAnnotationHeader[c]);
				}
				
				// Write QA slots
				for (int r = 0; r < maxNumQAs; r++) {
					row = sheet.createRow(rowCounter++);
					row.createCell(0).setCellValue("QA" + r);
					for (int c = 1; c < kAnnotationHeader.length; c++) {
						Cell cell = row.createCell(c);
						cell.setCellStyle(editable);
					}
					
					CellRangeAddressList trgCells = new CellRangeAddressList(),
										 ppCells = new CellRangeAddressList();
					
					int rn = row.getRowNum();
					whCells.addCellRangeAddress(rn, 1, rn, 1);
					auxCells.addCellRangeAddress(rn, 2, rn, 2);
					ph1Cells.addCellRangeAddress(rn, 3, rn, 3);
					trgCells.addCellRangeAddress(rn, 4, rn, 4);
					ph2Cells.addCellRangeAddress(rn, 5, rn, 5);
					ppCells.addCellRangeAddress(rn, 6, rn, 6);
					ph3Cells.addCellRangeAddress(rn, 7, rn, 7);
					for (int c = 9; c < 13; c++) {
						CellRangeAddressList ansCells = new CellRangeAddressList();
						Cell ansCell = row.getCell(c);
						ansCells.addCellRangeAddress(rn, c, rn, c);
						
						CellReference cref = new CellReference(ansCell);
						
						XSSFDataValidation ansVal =
							(XSSFDataValidation) dvHelper.createValidation(
								(XSSFDataValidationConstraint) dvHelper.createCustomConstraint(
									String.format("=FIND(TRIM(%s), \"%s\")",
											cref.formatAsString(), sent.getTokensString())),
							ansCells);
						
						ansVal.createErrorBox("Error", "Only use words in the sentence for answer");
						ansVal.setShowErrorBox(true);
						sheet.addValidationData(ansVal);
					}
					
					// Unit-specific validations
					XSSFDataValidation
						trgVal = (XSSFDataValidation) dvHelper.createValidation(
							trgConstraint, trgCells),
						ppVal = (XSSFDataValidation) dvHelper.createValidation(
							ppConstraint, ppCells);
						
					trgVal.createErrorBox("Error", "Input value not valid.");
					trgVal.setShowErrorBox(true);
					ppVal.createErrorBox("Error", "Input value not valid.");
					ppVal.setShowErrorBox(true);
					
					sheet.addValidationData(trgVal);
					sheet.addValidationData(ppVal);
				}
				
				// Write notes line
				row = sheet.createRow(rowCounter++);
				row.createCell(0).setCellValue("Feedback");
				Cell cell = row.createCell(1);
				cell.setCellStyle(editable);
				
				// Write separator .. whew
				row = sheet.createRow(rowCounter++);
			}
		}
       
        // Add WH, AUX, TRG, PH constraints.
        XSSFDataValidationConstraint
        	whConstraint = (XSSFDataValidationConstraint)
        		dvHelper.createExplicitListConstraint(QASlotQuestionWords.values),
        	auxConstraint = (XSSFDataValidationConstraint)
            	dvHelper.createExplicitListConstraint(QASlotAuxiliaryVerbs.values),
            phConstraint = (XSSFDataValidationConstraint)
            	dvHelper.createExplicitListConstraint(QASlotPlaceHolders.values),
            ph3Constraint = (XSSFDataValidationConstraint)
              	dvHelper.createExplicitListConstraint(QASlotPlaceHolders.ph3Values);
        
        XSSFDataValidation
        	whVal = (XSSFDataValidation) dvHelper.createValidation(whConstraint, whCells),
        	auxVal = (XSSFDataValidation) dvHelper.createValidation(auxConstraint, auxCells),
        	ph1Val = (XSSFDataValidation) dvHelper.createValidation(phConstraint, ph1Cells),
        	ph2Val = (XSSFDataValidation) dvHelper.createValidation(phConstraint, ph2Cells),
        	ph3Val = (XSSFDataValidation) dvHelper.createValidation(ph3Constraint, ph3Cells);
        
        whVal.createErrorBox("Error", "Input value not valid."); whVal.setShowErrorBox(true);
        auxVal.createErrorBox("Error", "Input value not valid."); auxVal.setShowErrorBox(true);
        ph1Val.createErrorBox("Error", "Input value not valid."); ph1Val.setShowErrorBox(true);
        ph2Val.createErrorBox("Error", "Input value not valid."); ph2Val.setShowErrorBox(true);
        ph3Val.createErrorBox("Error", "Input value not valid."); ph3Val.setShowErrorBox(true);
        
        sheet.addValidationData(whVal);
        sheet.addValidationData(auxVal);
        sheet.addValidationData(ph1Val);
        sheet.addValidationData(ph2Val);
        sheet.addValidationData(ph3Val);
        
        FileOutputStream out = new FileOutputStream(new File("test.xlsx"));
        workbook.write(out);
        out.close();
	}
	
	private static boolean isQuestion(DepSentence sentence) {
		for (int i = 0; i < sentence.length; i++) {
			String word = sentence.getTokenString(i);
			if (word.equals("?")) {
				return true;
			}
		}
		return false;
	}
	
	private static int[] getNonQuestionSentenceIds() {
		TIntArrayList ids = new TIntArrayList();
		for (DepSentence sentence : trainCorpus.sentences) {
			if (sentence.sentenceID > maxSentenceID) {
				break;
			}
			if (!isQuestion(sentence) && sentence.length >= 10) {
				ids.add(sentence.sentenceID);
			}
		}
		return ids.toArray();
	}
	
	private static HashSet<Integer> getAnnotatedSentenceIds() {
		HashSet<Integer> annotatedIds = new HashSet<Integer>();
		FileReader fileReader = null;
		Iterable<CSVRecord> records = null;
		for (String filePath : previousFileNames) {
			try {
				fileReader = new FileReader(filePath);
				records = CSVFormat.DEFAULT.withHeader().parse(fileReader);
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (CSVRecord record : records) {
				int sentId = Integer.parseInt(record.get("sent_id"));
				annotatedIds.add(sentId);
			}
		}
		return annotatedIds;
	}
	
	public static void main(String[] args) {
		trainCorpus = ExperimentUtils.loadSRLCorpus(
				ExperimentUtils.conll2009TrainFilename, "en-srl-train");
		inflDict = new VerbInflectionDictionary(trainCorpus);
		try {
			inflDict.loadDictionaryFromFile("wiktionary/en_verb_inflections.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		auxVerbID = new AuxiliaryVerbIdentifier(trainCorpus);
		sentences = new ArrayList<SRLSentence>();
		propositions = new ArrayList<ArrayList<Proposition>>();
		
		int[] nonQuestionIds = getNonQuestionSentenceIds();		
		System.out.println("Number of non-question sentences:\t" +
						   nonQuestionIds.length);
		
		// Exclude sentences that are already annotated ...
		HashSet<Integer> annotatedIds = getAnnotatedSentenceIds();
		
		// Throw away sentences that contains verbs we don't identify ...
		for (int id : nonQuestionIds) {
			if (annotatedIds.contains(id)) {
				continue;
			}
			SRLSentence sentence = (SRLSentence) trainCorpus.sentences.get(id);
			boolean containsUnidentifiedVerb = false;
			int numTargets = 0;
			for (int j = 0; j < sentence.length; j++) {
				if (sentence.getPostagString(j).equals("VERB") &&
					!auxVerbID.ignoreVerbForSRL(sentence, j)) {
					if (getTrgOptions(sentence, j) == null) {
						containsUnidentifiedVerb = true;
					}
					numTargets ++;
					break;
				}
			}
			if (!containsUnidentifiedVerb && numTargets > 0) {
				sentences.add(sentence);
			}
		}
		System.out.println("Sentences left:\t" + sentences.size());
		
		Collections.shuffle(sentences, new Random(randomSeed));
	
		for (int i = 0; i < sentences.size(); i++) {
			SRLSentence sentence = sentences.get(i);
			ArrayList<Proposition> props = new ArrayList<Proposition>();
			for (int j = 0; j < sentence.length; j++) {
				if (sentence.getPostagString(j).equals("VERB") &&
					!auxVerbID.ignoreVerbForSRL(sentence, j)) {					
					Proposition prop = new Proposition();
					prop.sentence = sentence;
					prop.setPropositionSpan(j, j + 1);
					props.add(prop);
				}
			}
			propositions.add(props);
		}
		
		try {
		//	outputUnits();
			outputXlsx();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}