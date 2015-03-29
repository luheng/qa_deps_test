package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import annotation.QASlotAuxiliaryVerbs;
import annotation.QASlotPlaceHolders;
import annotation.QASlotQuestionWords;

public class CrowdFlowerCMLGenerator {

	int maxNumQuestions;
	
	private static String dataStrHidden = "<p class=\"data-hidden\" id=\"s0\">{{orig_sent}}</p>\n\n";
	/*
	private static String dataTableStr = "<table cellpadding=\"10\">" +
			"<tr><th>Sentence:</th><td class=\"data-panel\">{{sentence}}</td></tr>" +
			"<tr><th>Target:</th><td class=\"data-panel\">{{proposition}}</td></tr>" +
			"</table><br>\n\n";
	*/
	private static String dataTableStr = "<table>" +
			"<tr><th>Sentence:</th><td class=\"data-panel\">{{sentence}}</td></tr>" +
			"</table><br>\n\n";

	private static String dataQuestionTableStr =  "<table>" +
			"<tr><th>Sentence:</th><td class=\"data-panel\">{{sentence}}</td></tr>" +
			"<tr><th>QA-%d:</th><td class=\"data-panel qa-panel\">" +
			"<div id=\"show_q%d\" class=\"written-question\"></div>" +
			"<div id=\"show_a%d\" class=\"written-answer\"></div></td></tr>" +
			"</table><br><br>\n\n";
	
	private static String liquidDeclareStr = "{% assign trg_ops = trg_options | split: \"#\" %}\n" +
			"{% assign pp_ops = pp_options | split: \"#\" %}\n";
	
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
	
	private String generateDynamicDropdown(String label, int questionId,
			String optionVar, String validator) {
		String str = "";
		if (validator.isEmpty()) {
			str += String.format("<cml:select label=\"%s\" name=\"%s\" class=\"cml-qslot\">",
				label, generateSlotName(label, questionId));
		} else {
			str += String.format("<cml:select label=\"%s\" name=\"%s\" class=\"cml-qslot\" validates=\"%s\">",
					label, generateSlotName(label, questionId), validator);
		}
		str += " {% for op in " + optionVar + " %}<cml:option label=\"{{op}}\"/>{% endfor %} ";
		str += "</cml:select>\n";
		return str;
	}
	
	private String generateQAString(int questionId) {
		String qstr = "";
		if (questionId > 0) {
			qstr += String.format("<cml:group only-if=\"!%s:unchecked\">\n",
					generateQuestionCheckerName(questionId));
			qstr += String.format(dataQuestionTableStr, questionId + 1,
					questionId, questionId);
		}
		
		qstr += String.format("<!--  question %d -->\n", questionId);
		
		qstr += "<label class=\"q-panel-label\">Question:</label>\n";
		
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
		/*
		qstr += String.format("<cml:text label=\"%s\" name=\"%s\" class=\"cml-qslot\" validates=\"required\"/>",
							  trgSlotLabel,
							  generateSlotName(trgSlotLabel, questionId));
		*/
		qstr += generateDynamicDropdown(trgSlotLabel,
				 						questionId,
				 						"trg_ops",
				 						"required");
		
		// Generate ph2 slot
		qstr += generateDropdown(ph2SlotLabel,
				 				 questionId,
				 				 QASlotPlaceHolders.values,
								 "" /* no validator */);
		
		// Generate pp slot
		qstr += generateDynamicDropdown(ppSlotLabel,
									    questionId,
									    "pp_ops",
								 		"" /* no validator */);
		
		// Generate ph3 slot
		qstr += generateDropdown(ph3SlotLabel,
				 				 questionId,
				 				 QASlotPlaceHolders.ph3Values,
				 				 "" /* no validator */);
		
		qstr += "<strong>?</strong><br>\n";
		
		// Generate show-question box
		/*
		qstr += String.format("<label class=\"show_q_label\" for=\"show_q%d\">Question:</label><div id=\"show_q%d\"></div>\n",
							  questionId, questionId);
		*/
		
		// Generate answer slot
		qstr += String.format("<cml:text label=\"Answer:\" name=\"a%d\" class=\"cml-aslot\" " +
							  "validates=\"required yext_no_international_url\" multiple=\"true\"/>\n",
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
	
	private String generateFreeFormQAString(int questionId) {
		String qstr = "";
		
		String qChecker = generateQuestionCheckerName(questionId);
		qstr += String.format("<cml:checkbox label=\"Feedback (if you have QA-pair that cannot fit into the template)\" class=\"cml-chk\" name=\"%s\"/>",
				  			  qChecker);
		
		qstr += String.format("<cml:group only-if=\"!%s:unchecked\">\n", qChecker);
		qstr += dataTableStr;
		qstr += String.format("<!--  question %d -->\n", questionId);
				
		// Generate question slot
		qstr += String.format("<cml:text label=\"%s\" name=\"q%dff\" class=\"cml-ffqslot\"/>",
							  "Question", questionId); // "ff" stands for "free-form"
		
		// Generate answer slot
		qstr += String.format("<cml:text label=\"Answer:\" name=\"a%d\" class=\"cml-aslot\" multiple=\"true\"/>\n",
							  questionId);
		
		// Generate feedback slot
		qstr += "<cml:text label=\"Other Feedback:\" name=\"fdbk\" class=\"cml-ffqslot\"/>\n";
		
		qstr += "<hr></cml:group>";
		
		return qstr;
	}
	
	public void generateCML(String outputFileName) throws IOException {
		FileWriter fileWriter = new FileWriter(outputFileName);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
		bufferedWriter.write(dataStrHidden);
		bufferedWriter.write(String.format(dataQuestionTableStr, 1, 0, 0));
		bufferedWriter.write(liquidDeclareStr);
		
		
		//bufferedWriter.write("<cml:group only-if=\"check_noq:unchecked\">\n");
		for (int i = 0; i < maxNumQuestions; i++) {
			bufferedWriter.write(generateQAString(i) + "\n\n");
		}
		//bufferedWriter.write("<hr></cml:group>\n");
		
		bufferedWriter.write("<hr>\n");
		bufferedWriter.write("<cml:checkbox label=\"Difficult to come up with questions/Not confident with the questions asked.\" " +
				 "class=\"cml-chk cml-chk-noq\" name=\"check_noq\"/>\n");
		bufferedWriter.write(generateFreeFormQAString(maxNumQuestions) + "\n\n");
		
		bufferedWriter.close();
		System.out.println("Successfully generated CML template to "
				+ outputFileName);
	}
	
	public CrowdFlowerCMLGenerator(int maxNumQuestions) {
		this.maxNumQuestions = maxNumQuestions;
	}
	
	public static void main(String[] args) {
		CrowdFlowerCMLGenerator cmlGen = new CrowdFlowerCMLGenerator(8);
		try {
			cmlGen.generateCML("crowdflower/cml_qa_new_8q.html");			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
