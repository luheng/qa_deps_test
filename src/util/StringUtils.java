package util;

import java.util.ArrayList;

public class StringUtils {
	public static String join(String delimiter, String[] stringArr) {
		String joined = "";
		for (int i = 0; i < stringArr.length; i++) {
			if (!stringArr[i].trim().isEmpty()) {
				if (!joined.isEmpty()) {
					joined += delimiter;
				}
				joined += stringArr[i];
			}
		}
		return joined;
	}
	
	public static String numberedJoin(String delimiter, String[] stringArr) {
		String joined = "";
		for (int i = 0; i < stringArr.length; i++) {
			if (!stringArr[i].trim().isEmpty()) {
				if (!joined.isEmpty()) {
					joined += delimiter;
				}
				joined += stringArr[i] + "(" + i + ")";
			}
		}
		return joined;
	}
	
	public static String join(String delimiter, String[] stringArr,
			int startIdx, int endIdx) {
		String joined = "";
		for (int i = startIdx; i < endIdx && i < stringArr.length; i++) {
			if (!stringArr[i].trim().isEmpty()) {
				if (!joined.isEmpty()) {
					joined += delimiter;
				}
				joined += stringArr[i];
			}
		}
		return joined;
	}
	
	public static String intArrayToString(String delimiter, int[] intArr) {
		String joined = "";
		for (int i = 0; i < intArr.length; i++) {
			if (i > 0) {
				joined += delimiter;
			}
			joined += intArr[i];
		}
		return joined;
	}
	
	public static String doubleArrayToString(String delimiter,
										     double[] doubleArr) {
		String joined = "";
		for (int i = 0; i < doubleArr.length; i++) {
			if (i > 0) {
				joined += delimiter;
			}
			joined += String.format("%.3f", doubleArr[i]);
		}
		return joined;
	}
	
	public static boolean isEmptyStringArray(ArrayList<String> strArray) {
		for (String str : strArray) {
			if (!str.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
