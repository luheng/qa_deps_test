package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotPrepositions;
import annotation.QASlotQuestionWords;

public class CrowdFlowerCMLGenerator {

	int maxNumQuestions = 10;
	
	private static String dataTableStr = "<table cellpadding=\"10\">" +
			"<tr><th>Sentence:</th><td class=\"data-panel\">{{sentence}}</td></tr>" +
			"<tr><th>Target:</th><td class=\"data-panel\">{{proposition}}</td></tr>" +
			"</table><br>\n\n";
	
	private static String dataStrCompact =  "<table cellpadding=\"10\">" +
			"<tr><th>Sentence repeated:</th><td class=\"data-panel\">{{sentence}}</td></tr>" +
			"</table><br><br>\n";
	
	private static String whSlotLabel = "WH",
						  auxSlotLabel = "AUX",
						  ph1SlotLabel = "PH1",
						  trgSlotLabel = "TRG",
						  ph2SlotLabel = "PH2",
						  ppSlotLabel = "PP",
						  ph3SlotLabel = "PH3";
	
	private String generateSlotName(String slotLabel, int questionId) {
		return String.format("q%d%s", questionId, slotLabel.toLowerCase());		
	}
	
	private String generateQuestionCheckerName(int questionId) {
		return "check_q" + questionId;
	}
	
	private String generateOptionString(String label) {
		return String.format("<cml:option label=\"%s\"/>", label);
	}
	
	private String generateDropdown(String label, int questionId,
			String[] options, String validator) {
		String str = "";
		if (validator.isEmpty()) {
			str += String.format("<cml:select label=\"%s\" name=\"%s\" class=\"cml-qslot\">",
				label, generateSlotName(label, questionId));
		} else {
			str += String.format("<cml:select label=\"%s\" name=\"%s\" class=\"cml-qslot\" validates=\"%s\">",
					label, generateSlotName(label, questionId), validator);
		}
		for (String optionStr : options) {
			str += generateOptionString(optionStr) + " ";
		}
		str += "</cml:select>\n";
		return str;
	}
	
	private String generateQAString(int questionId) {
		String qstr = "";
		if (questionId > 0) {
			qstr += String.format("<cml:group only-if=\"!%s:unchecked\">\n",
					generateQuestionCheckerName(questionId));
		}
		
		if (questionId > 0 && questionId % 2 == 0) {
			qstr += dataStrCompact;
		}
		
		qstr += String.format("<!--  question %d -->\n", questionId);
		
		// Generate wh slot
		qstr += generateDropdown(whSlotLabel,
								 questionId,
								 QASlotQuestionWords.values,
								 "required");
		
		// Generate aux slot
		qstr += generateDropdown(auxSlotLabel,
								 questionId,
								 QASlotAuxiliaryVerbs.values,
								 "" /* no validator */);
		
		// Generate ph1 slot
		qstr += generateDropdown(ph1SlotLabel,
				 				 questionId,
				 				 QASlotPlaceHolders.values,
								 "" /* no validator */);

		// Generate trg slot
		qstr += String.format("<cml:text label=\"%s\" name=\"%s\" class=\"cml-qslot\" validates=\"required\"/>",
							  trgSlotLabel,
							  generateSlotName(trgSlotLabel, questionId));
		
		// Generate ph2 slot
		qstr += generateDropdown(ph2SlotLabel,
				 				 questionId,
				 				 QASlotPlaceHolders.values,
								 "" /* no validator */);
		
		// Generate pp slot
		qstr += generateDropdown(ppSlotLabel,
								 questionId,
								 QASlotPrepositions.values,
								 "" /* no validator */);
		
		// Generate ph3 slot
		qstr += generateDropdown(ph3SlotLabel,
				 				 questionId,
				 				 QASlotPlaceHolders.values,
				 				 "" /* no validator */);
		
		qstr += "<strong>?</strong><br>\n";
		
		// Generate answer slot
		qstr += String.format("<cml:text label=\"Answer:\" name=\"a%d\" class=\"cml-aslot\" validates=\"required\" multiple=\"true\"/>\n",
							  questionId);
	
		// Use a seperator :)
		qstr += "<hr>";
		
		if (questionId > 0) {
			qstr += "</cml:group>";
		}
		
		// Generate question checkbox
		int nextQuestionId = questionId + 1;
		if (nextQuestionId < maxNumQuestions) {
			qstr += String.format("<cml:checkbox label=\"Input Question No. %d\" class=\"cml-chk\" name=\"%s\"/>",
								  nextQuestionId + 1,
								  generateQuestionCheckerName(nextQuestionId));
		}
		return qstr;
	}
	
	public void generateCML(String outputFileName) throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
		bufferedWriter.write(dataTableStr);
		
		for (int i = 0; i < maxNumQuestions; i++) {
			bufferedWriter.write(generateQAString(i) + "\n\n");
		}
		
		bufferedWriter.close();
		System.out.println("Successfully generated CML template to "
				+ outputFileName);
	}
	
	public CrowdFlowerCMLGenerator(int maxNumQuestions) {
		this.maxNumQuestions = maxNumQuestions;
	}
	
	public static void main(String[] args) {
		CrowdFlowerCMLGenerator cmlGen = new CrowdFlowerCMLGenerator(10);
		try {
			cmlGen.generateCML("crowdflower/cml_10q.html");			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
