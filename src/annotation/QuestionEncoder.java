package annotation;

import java.util.Arrays;

import data.QAPair;
import data.Sentence;

/* Encode the question into information we want.
 *  
 */
public class QuestionEncoder {
	
	public static String encode(String[] question, Sentence sentence) {
		String[] tokens = new String[sentence.length];
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = sentence.getTokenString(i);
		}
		return encode(question, tokens);
	}
	
	/**
	 * 
	 * @param question 
	 * in its 7-slots template: 
	 * [wh] [aux] [ph1] [trg] [ph2] [pp] [ph3]
	 * 
	 */
	public static String encode(String[] question, String[] tokens) {	
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
		/*
		boolean hasInformativePP = false;
		if (tokens != null) {
			for (String tok : tokens) {
				if (tok.toLowerCase().equals(pp)) {
					hasInformativePP = true;
					break;
				}
			}
		}
		*/
		String label = null;
		if (wh.equals("whom")) {
			wh = "who";
		}
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
				// label = wh + "_do";
				label = wh + "_2";
		    } else if (nullPh3 && !nullPh2 && !nullPh1 && !passiveVoice) {
				// e.g. Who did someone give something to?
				label = wh + "_2";
			} else if (nullPh3 && nullPh2 && !nullPh1 && passiveVoice) {
				// e.g. What is something capped to?
				//      Who is something baked for?
				//      What is something being driven for?
				//      What is someone given?
				//      What is someone named?
				label = wh + "_2";
			} else {
				label = wh + "_???";
			}
		} else {
			label = wh;
			if (wh.equals("where") && !pp.isEmpty() && nullPh3) {
				label += "_" + pp;
			}
		}
		
		// Negation.
		if (aux.contains("not") || aux.contains("n\'t")) {
			label += "_n";
		}
		//System.out.println(StringUtils.join(" ", question));
		//System.out.println(label + "\n");
		return label;
	}
	
	public static String[] encode(String[] question) {	
		assert (question.length == 7);
		String wh  = question[0],
			   aux = question[1],
			   ph1 = question[2],
			   trg = question[3],
			   ph2 = question[4],
			   pp  = question[5],
			   ph3 = question[6];
		boolean nullPh1 = ph1.isEmpty(),
				nullPh2 = ph2.isEmpty(), // || ph2.equals("somewhere"),
				nullPh3 = ph3.isEmpty(), // || ph3.equals("somewhere"),
				nullPP = pp.isEmpty();
		boolean verbalPh3 = (ph3.equals("do") || ph3.equals("doing") || ph3.equals("be"));
		boolean passiveVoice = isPassiveVoice(aux, trg);
		if (wh.equals("whom")) {
			wh = "who";
		}
		String[] labels = new String[4];
		Arrays.fill(labels, "");
		labels[0] = wh;
		if (isWhoWhat(wh)) {
			if (nullPh1 && !passiveVoice) {
				// e.g. Who built something? What dropped?
				labels[1] = "0";
			} else if (nullPh2 && nullPh1 && passiveVoice) {
				// e.g. Who is killed? Who is expected to do something?
				//      What is given? What is given by someone?
				//      What is given to someone?
				labels[1] = "1";
			} else if (nullPh2 && !nullPh1 && !passiveVoice) {
				// e.g. Who did someone kill?
				//      What did someone give (to someone)?
				labels[1] = "1";
			} else if (!nullPh2 && nullPh1 && passiveVoice) {
				// e.g. Who is given something?
				//      Who is given something by someone?
				labels[1] = "2";
			} else if (verbalPh3 && !nullPh1) {
				// e.g. What does someone expect someone to do?
				//      What was someone expected to do?
				//      What did someone threaten to do?
				// label = wh + "_do";
				labels[1] = "2";
		    } else if (nullPh3 && !nullPh2 && !nullPh1) {
				// e.g. Who did someone give something to?
		    	labels[1] = "2";
			} else if (nullPh3 && nullPh2 && !nullPh1 && passiveVoice) {
				// e.g. What is something capped to?
				//      Who is something baked for?
				//      What is something being driven for?
				//      What is someone given?
				//      What is someone named?
				labels[1] = "2";
			} else {
				labels[1] = "3"; // Unknown
			}
			if (labels[1].equals("2") && !nullPP && nullPh3) {
				labels[2] = pp;
			}
		} else {
			if (!nullPP && nullPh3) {
				labels[2] = pp;
			}
		}
		
		if (aux.contains("not") || aux.contains("n\'t")) {
			labels[3] = "N";
		}
		return labels;
	}
	
	
	public static String encodeLong(String[] question) {	
		String encoded = "";
		assert (question.length == 7);
		String wh  = question[0],
			   aux = question[1],
			   ph1 = question[2],
			   trg = question[3],
			   ph2 = question[4],
			   pp  = question[5],
			   ph3 = question[6];
		if (wh.equals("whom")) {
			wh = "who";
		}
		/*
		if (ph3.equals("do something") || ph3.equals("doing something") ||
			ph3.equals("be something") || ph3.equals("being something")) {
			ph3 = "do something";
		}
		if (ph3.equals("do") || ph3.equals("doing") || ph3.equals("be") ||
			ph3.equals("being")) {
			ph3 = "do";
		}
		*/
		boolean passiveVoice = isPassiveVoice(aux, trg);
		encoded = wh.toLowerCase() + "_"
				+ (passiveVoice ? "passive" : "active") + "_"
				+ nonEmptyOr(ph1, ph1, "nullph1") + "_"
				+ nonEmptyOr(ph2, ph2, "nullph2") + "_"
				+ nonEmptyOr(pp, pp, "nullpp") + "_"
				+ nonEmptyOr(ph3, ph3, "nullph3");
		return encoded;
	}
	
	/**
	 * Generate question labels with multiple granularity. (maybe three)
	 * @param question. i.e. Who wouldn't build something?
	 * @return multi-granularity question labels. i.e. [who, who_0, who_0_n] 
	 */
	public static String[] getMultiQuestionLabels(String[] question, QAPair qa) {
		String[] qlabels = encode(question);
		String[] labels = new String[qlabels.length];
		labels[0] = qlabels[0];
		for (int i = 1; i < labels.length; i++) {
			labels[i] = labels[i-1];
			if (!qlabels[i].isEmpty()) {
				labels[i] += "_" + qlabels[i];
			}
		}
		return labels;
	}
	
	private static String nonEmptyOr(String str, String r1, String r2) {
		return !str.isEmpty() ? r1 : r2;
	}
	
	public static boolean isPassiveVoice(String[] question) {
		return isPassiveVoice(question[1].toLowerCase(),
				question[3].toLowerCase());
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
