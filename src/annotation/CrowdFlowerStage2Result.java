package annotation;

import org.apache.commons.csv.CSVRecord;


// CSV header:
// _unit_id,_created_at,_golden,_id,_missed,_started_at,_tainted,_channel,
// _trust,_worker_id,_country,_region,_city,_ip,
// ans,radio_ans,radio_reason,ans_gold,
// orig_question,orig_sent,prop_end,prop_id,prop_start,proposition,
// question,question_label,radio_ans_gold,radio_reason_gold,
// sent_id,sentence,stage1_id
public class CrowdFlowerStage2Result extends CrowdFlowerResult {
	// Used to align with original data.
	public int sentenceId, propositionId, propStart, propEnd, propHead,
			   stage1Id;
	public String proposition;

	public String question, qlabel;
	// Actual annotation result.
	
	public boolean hasAnswer, badQuestion, isGold;
	public String[] answers;
	
	protected CrowdFlowerStage2Result() {
		super();
	}
	
	public static CrowdFlowerStage2Result parseCSV(CSVRecord csvRecord) {
		CrowdFlowerStage2Result result = new CrowdFlowerStage2Result();
		CrowdFlowerResult.parseCSV(csvRecord, result);
		
		// Parse original data information.
		result.sentenceId = Integer.parseInt(csvRecord.get("sent_id"));
		result.propositionId = Integer.parseInt(csvRecord.get("prop_id"));
		result.proposition = csvRecord.get("proposition");
		result.propEnd = Integer.parseInt(csvRecord.get("prop_end"));
		result.propStart = -1;
		if (csvRecord.isMapped("prop_head")) {
			result.propHead = Integer.parseInt(csvRecord.get("prop_head"));
		} else {
			result.propHead = -1;
		}
		result.question = csvRecord.get("orig_question");
		result.qlabel = csvRecord.get("question_label");
		result.stage1Id = Integer.parseInt(csvRecord.get("stage1_id"));
		
		// Parse annotation result.
		result.hasAnswer = csvRecord.get("radio_ans").contains("Yes");
		result.badQuestion = csvRecord.get("radio_reason").contains("incomprehensible");
		result.answers = csvRecord.get("ans").split("\n");
		
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
		// TODO ...
		result += "\n";
		
		return result;
	}
}
