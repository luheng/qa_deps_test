package annotation;

import java.util.HashMap;

public class PTBTokens {

	public static final HashMap<String, String> tokenMap;
	static {
		tokenMap = new HashMap<String, String>();
		tokenMap.put("-LRB-", "(");
		tokenMap.put("-RRB-", ")");
		tokenMap.put("-LSB-", "[");
		tokenMap.put("-RSB-", "]");
		tokenMap.put("-LCB-", "{");
		tokenMap.put("-RCB-", "}");
	}
}
