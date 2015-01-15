package annotation;

import java.util.ArrayList;

import org.apache.commons.csv.CSVRecord;

import util.StringUtils;

public class CrowdFlowerQAResult {

	// CrowdFlower specific information.
	public int cfUnitId, cfWorkerId;
	public String cfChannel, cfCountry, cfRegion;
	public float cfTrust;
	
	// Used to align with original data.
	public int sentenceId, propositionId;
	public String proposition;
	
	// Actually annotation result.
	public ArrayList<String[]> questions;
	public ArrayList<String> answers;
	public String ffQuestion, ffAnswer, feedback;
	
	private CrowdFlowerQAResult() {
		questions = new ArrayList<String[]>();
		answers = new ArrayList<String>();
	}
	
	private static final int maxNumQA = 10;
	private static final int numQuestionSlots = 7;
	private static final String[] slotNames = {"wh", "aux", "ph1", "trg", "ph2",
											   "pp", "ph3"};
	
	private static String getSlotName(int questionId, int slotId) {
		return "q" + questionId + slotNames[slotId];
	}
	
	public static CrowdFlowerQAResult parseCSV(CSVRecord csvRecord) {
		CrowdFlowerQAResult result = new CrowdFlowerQAResult();
		
		// Parse CrowdFlower specific information.
		result.cfUnitId = Integer.parseInt(csvRecord.get("_unit_id"));
		result.cfWorkerId = Integer.parseInt(csvRecord.get("_worker_id"));
		result.cfChannel = csvRecord.get("_channel");
		result.cfCountry = csvRecord.get("_country");
		result.cfRegion = csvRecord.get("_region");
		result.cfTrust = Float.parseFloat(csvRecord.get("_trust"));
		
		// Parse original data information.
		result.sentenceId = Integer.parseInt(csvRecord.get("sent_id"));
		result.propositionId = Integer.parseInt(csvRecord.get("prop_id"));
		result.proposition = csvRecord.get("proposition");
		
		// Parse annotation result.
		for (int i = 0; i < maxNumQA; i++) {
			String whSlot = getSlotName(i, 0);
			if (!csvRecord.isMapped(whSlot)) {
				break;
			}
			if (!csvRecord.isSet(whSlot) || csvRecord.get(whSlot).isEmpty()) {
				continue;
			}
			String[] qWords = new String[numQuestionSlots];
			for (int j = 0; j < numQuestionSlots; j++) {
				qWords[j] = csvRecord.get(getSlotName(i, j));
			}
			String answer = csvRecord.get("a" + i);
			result.questions.add(qWords);
			result.answers.add(answer);
		}
		
		// Get free-form QA, and feedback.
		result.ffQuestion = csvRecord.get("q" + maxNumQA + "ff");
		result.ffAnswer = csvRecord.get("a" + maxNumQA);
		result.feedback = csvRecord.get("fdbk");
		return result;
	}
	
	@Override
	public String toString() {
		String result = "";
		result += String.format("unitId: %d\tworkerId: %d\ttrust: %.3f\n",
								cfUnitId, cfWorkerId, cfTrust);
		result += String.format("channel: %s\tlocation: %s\n",
								cfChannel, cfCountry + cfRegion);
		result += String.format("sentId: %d\tpropId: %d\tprop: %s\n",
								sentenceId, propositionId, proposition);
		for (int i = 0; i < questions.size(); i++) {
			result += StringUtils.join(" ", questions.get(i)) + "?\t" +
					  answers.get(i) + "\n";
		}
		if (!ffQuestion.isEmpty() || !ffAnswer.isEmpty() ||
			!feedback.isEmpty()) {
			result += "*Feedback:\t" + ffQuestion + "\t" + ffAnswer + "\t" +
					  feedback;
		}
		result += "\n";
		
		return result;
	}
}
