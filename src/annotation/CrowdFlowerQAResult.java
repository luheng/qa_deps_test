package annotation;

import java.util.ArrayList;

import org.apache.commons.csv.CSVRecord;

import util.StrUtils;

public class CrowdFlowerQAResult extends CrowdFlowerResult {
	private static final long serialVersionUID = 1L;
	// Used to align with original data.
	public int sentenceId, propositionId, propStart, propEnd, propHead;
	public String proposition;
	
	// Actual annotation result.
	public ArrayList<String[]> questions; // multiple slots of each question
	public ArrayList<String[]> answers;   // multiple versions of each answer
	public String ffQuestion, ffAnswer, feedback;
	
	protected CrowdFlowerQAResult() {
		super();
		questions = new ArrayList<String[]>();
		answers = new ArrayList<String[]>();
	}

	public static final int maxNumQA = 10;
	private static final int numQuestionSlots = 7;
	private static final String[] slotNames = {"wh", "aux", "ph1", "trg", "ph2",
											   "pp", "ph3"};
	
	private static String getSlotName(int questionId, int slotId) {
		return "q" + questionId + slotNames[slotId];
	}
	
	public static CrowdFlowerQAResult parseCSV(CSVRecord csvRecord) {
		CrowdFlowerQAResult result = new CrowdFlowerQAResult();
		CrowdFlowerResult.parseCSV(csvRecord, result);

		// Parse original data information.
		result.sentenceId = Integer.parseInt(csvRecord.get("sent_id"));
		result.propositionId = Integer.parseInt(csvRecord.get("prop_id"));
		result.proposition = csvRecord.get("proposition");
		if (csvRecord.get("prop_start") != null) {
			result.propStart = Integer.parseInt(csvRecord.get("prop_start"));
			result.propEnd = Integer.parseInt(csvRecord.get("prop_end"));
		} else {
			result.propStart = result.propEnd = -1;
		}
		if (csvRecord.isMapped("prop_head")) {
			result.propHead = Integer.parseInt(csvRecord.get("prop_head"));
		} else {
			result.propHead = -1;
		}
		
		// Parse annotation result.
		int qaCnt = 0;
		for ( ; qaCnt < maxNumQA; qaCnt++) {
			String whSlot = getSlotName(qaCnt, 0);
			if (!csvRecord.isMapped(whSlot)) {
				break;
			}
			if (!csvRecord.isSet(whSlot) || csvRecord.get(whSlot).isEmpty()) {
				continue;
			}
			String[] qWords = new String[numQuestionSlots];
			for (int j = 0; j < numQuestionSlots; j++) {
				qWords[j] = csvRecord.get(getSlotName(qaCnt, j));
			}
			String[] answerStrs = csvRecord.get("a" + qaCnt).split("\n");
			result.questions.add(qWords);
			result.answers.add(answerStrs);
		}
		
		// Get free-form QA, and feedback.
		result.ffQuestion = csvRecord.get("q" + qaCnt + "ff");
		result.ffAnswer = csvRecord.get("a" + qaCnt);
		result.feedback = csvRecord.get("fdbk");
		return result;
	}
	
	public int getSentId() {
		return sentenceId;
	}
	
	public int getPropHead() {
		return propHead >= 0 ? propHead : propEnd - 1;
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
			result += StrUtils.join(" ", questions.get(i)) + "?\t" +
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
