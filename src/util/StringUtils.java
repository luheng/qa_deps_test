package util;

public class StringUtils {
	public static String join(String delimiter, String[] stringArr) {
		String joined = "";
		for (int i = 0; i < stringArr.length; i++) {
			if (i > 0) {
				joined += delimiter;
			}
			joined += stringArr[i];
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
}
