package annotation;

import util.StringUtils;

/* Encode the question into information we want.
 *  
 */
public class QuestionEncoder {
	/**
	 * 
	 * @param question 
	 * in its 7-slots template: 
	 * [wh] [aux] [ph1] [trg] [ph2] [pp] [ph3]
	 * 
	 */
	public static String encode(String[] question) {	
		assert (question.length == 7);
		
		String wh  = question[0],
			   aux = question[1],
			   ph1 = question[2],
			   trg = question[3],
			   ph2 = question[4],
			   pp  = question[5],
			   ph3 = question[6];
		
		boolean nullPh1 = ph1.isEmpty(),
				nullPh2 = ph2.isEmpty() || ph2.equals("somewhere"),
				nullPh3 = ph3.isEmpty() || ph3.equals("somewhere");
		
		boolean passiveVoice = isPassiveVoice(aux, trg);
		String label = null;
		
		if (isWhoWhat(wh)) {
			if (nullPh1 && !passiveVoice) {
				// e.g. Who built something? What dropped?
				label = wh + "_0";
			} else if (nullPh2 && nullPh1 && passiveVoice) {
				// e.g. Who is killed? Who is expected to do something?
				//      What is given? What is given by someone?
				//      What is given to someone?
				label = wh + "_1";
			} else if (nullPh2 && !nullPh1 && !passiveVoice) {
				// e.g. Who did someone kill?
				//      What did someone give (to someone)?
				label = wh + "_1";
			} else if (!nullPh2 && nullPh1 && passiveVoice) {
				// e.g. Who is given something?
				//      Who is given something by someone?
				label = wh + "_2";
			} else if (ph3.equals("do") && !nullPh1) {
				// e.g. What does someone expect someone to do?
				//      What was someone expected to do?
				//      What did someone threaten to do?
				label = wh + "_do";
		    } else if (nullPh3 && !nullPh2 && !nullPh1 && !passiveVoice) {
				// e.g. Who did someone give something to?
				label = wh + "_2";
			} else if (nullPh3 && nullPh2 && !nullPh1 && passiveVoice && !pp.isEmpty()) {
				// e.g. What is something capped to?
				//      Who is something baked for?
				//      What is something being driven for?
				label = wh + "_2_" + pp;
			} else {
				//System.out.println(passiveVoice ? "passive" : "active");
				//System.out.println(ph1 + " " + ph2 + " " + ph3);
				label = wh + "_?????";
				//System.out.println(StringUtils.join(" ", question));
				//System.out.println(label + "\n");
			}
		} else {
			if (pp.isEmpty()) {
				label = wh;
			} else {
				label = wh + "_" + pp;
			}
		}
		//System.out.println(StringUtils.join(" ", question));
		//System.out.println(label + "\n");
		return label;
	}
	
	private static boolean isPassiveVoice(String aux, String trg) {
		if (!trg.endsWith("ing") && (
				trg.startsWith("have been") ||
				trg.startsWith("be ") ||
				trg.startsWith("been ") ||
				trg.startsWith("being "))) {
			// e.g. have been broken, been broken, be broken, being broken
			return true;
		}
		if (!trg.endsWith("ing") && (
				aux.startsWith("is") ||
				aux.startsWith("are") ||
				aux.startsWith("was") ||
				aux.startsWith("were"))) {
			return true;
		}
		return false;
	}
	
	private static boolean isWhoWhat(String wh) {
		// Assume people are careless about using whom.
		return wh.equalsIgnoreCase("who") || wh.equalsIgnoreCase("what") ||
			   wh.equalsIgnoreCase("whom");		
	}
}
