package annotation;

import java.util.ArrayList;

import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import util.StringUtils;

public class CrowdFlowerResult {

	// CrowdFlower specific information.
	public int cfUnitId, cfWorkerId;
	public String cfChannel, cfCountry, cfRegion;
	public float cfTrust;

	// Other annotation information, including start and end time
	public DateTime cfCreateTime, cfStartTime;
	public int secondsToComplete; // in seconds
	
	protected CrowdFlowerResult() {
	}
	
	protected static final DateTimeFormatter cfDateTimeFormatter =
			DateTimeFormat.forPattern("m/dd/yy HH:mm:ss");
	
	public static CrowdFlowerResult parseCSV(CSVRecord csvRecord) {
		CrowdFlowerResult result = new CrowdFlowerResult();
		
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
		return result;
	}
	
	@Override
	public String toString() {
		String result = "";
		result += String.format("unitId: %d\tworkerId: %d\ttrust: %.3f\n",
								cfUnitId, cfWorkerId, cfTrust);
		result += String.format("channel: %s\tlocation: %s\n",
								cfChannel, cfCountry + cfRegion);
		result += "\n";
		
		return result;
	}
}
