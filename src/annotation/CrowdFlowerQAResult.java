package annotation;

import java.util.ArrayList;

import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import util.StringUtils;

public class CrowdFlowerQAResult {

	// CrowdFlower specific information.
	public int cfUnitId, cfWorkerId;
	public String cfChannel, cfCountry, cfRegion;
	public float cfTrust;

	// Other annotation information, including start and end time
	public DateTime cfCreateTime, cfStartTime;
	public int secondsToComplete; // in seconds
	
	// Used to align with original data.
	public int sentenceId, propositionId, propStart, propEnd, propHead;
	public String proposition;
	
	// Actual annotation result.
	public ArrayList<String[]> questions; // multiple slots of each question
	public ArrayList<String[]> answers;   // multiple versions of each answer
	public String ffQuestion, ffAnswer, feedback;
	
	private CrowdFlowerQAResult() {
		questions = new ArrayList<String[]>();
		answers = new ArrayList<String[]>();
	}
	
	private static final DateTimeFormatter cfDateTimeFormatter =
			DateTimeFormat.forPattern("m/dd/yy HH:mm:ss");
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
		
		result.cfStartTime = cfDateTimeFormatter.parseDateTime(
				csvRecord.get("_started_at"));
		result.cfCreateTime = cfDateTimeFormatter.parseDateTime(
				csvRecord.get("_created_at"));
		result.secondsToComplete = (int) (result.cfCreateTime.getMillis() -
				result.cfStartTime.getMillis()) / 1000;
		
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
		if (csvRecord.get("prop_head") != null) {
			result.propHead = Integer.parseInt(csvRecord.get("prop_head"));
		} else {
			result.propHead = -1;
		}
		
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
			String[] answerStrs = csvRecord.get("a" + i).split("\n");
			result.questions.add(qWords);
			result.answers.add(answerStrs);
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
