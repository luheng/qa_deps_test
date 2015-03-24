package annotation;

import java.util.ArrayList;

import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import util.StringUtils;

public class CrowdFlowerStage2Result extends CrowdFlowerResult {
	// Used to align with original data.
	public int sentenceId, propositionId, propStart, propEnd, propHead;
	public String proposition;

	// TODO: question ID?
	public String question;
	// Actual annotation result.
	
	public String[] answers;   // multiple versions of each answer
	// TODO: no answer reason
	
	private CrowdFlowerStage2Result() {
	}
	
	public static CrowdFlowerStage2Result parseCSV(CSVRecord csvRecord) {
		CrowdFlowerStage2Result result =
				(CrowdFlowerStage2Result) CrowdFlowerResult.parseCSV(csvRecord);
		
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
		// TODO ...
		result += "\n";
		
		return result;
	}
}
