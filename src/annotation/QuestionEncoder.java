package annotation;

import java.util.Arrays;

import data.QAPair;
import data.Sentence;


/* Encode the question into information we want.
 *  
 */
public class QuestionEncoder {
	
	public static void normalize(String[] question) {
		String ph2 = question[QASlots.PH2SlotId],
			   pp  = question[QASlots.PPSlotId],
			   ph3 = question[QASlots.PH3SlotId];
		
		if (QASlotPlaceHolders.valueSet.contains(ph3) && !ph3.isEmpty() &&
			ph2.isEmpty() && pp.isEmpty()) {
			question[QASlots.PH2SlotId] = question[QASlots.PH3SlotId];
			question[QASlots.PH3SlotId] = "";
		}
	}
	
	
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
	
	/**
	 * 
	 * @param question
	 * @return String[] : [Wh, arg#, arg_label, pp, modal, negation]
	 */
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
				nullPh2 = ph2.isEmpty(),
				nullPh3 = ph3.isEmpty(),
				nullPP = pp.isEmpty();
		boolean verbalPh3 = (ph3.equals("do") || ph3.equals("doing") || ph3.equals("be"));
		boolean passiveVoice = isPassiveVoice(aux, trg),
				activeVoice = !passiveVoice;
		
		String[] labels = new String[6];
		Arrays.fill(labels, "");
		labels[0] = wh;
		if (isWhoWhat(wh)) {
			if (activeVoice && nullPh1) {
				labels[1] = "0";
			} else if ((activeVoice && nullPh2) || (passiveVoice && nullPh1)) {
				labels[1] = "1";
			} else if ((activeVoice && !nullPh1 && !nullPh2) ||
    				   (passiveVoice && !nullPh1 && nullPh2)) {
				labels[1] = "2";
			} else {
				labels[1] = "?";
			}
			// if (activeVoice && !nullPh1 && !nullPh2 && nullPP && nullPh3) {
			// 	System.out.println(StringUtils.join("\t", question));
			// 	}
		} 
		if (!nullPP && nullPh3) {
			labels[2] = pp;
		}
		if (verbalPh3) {
			labels[2] = "do";
		}
		labels[3] = pp;
		labels[4] = aux.contains(" ") ? aux.split(" ")[0] : "";
		labels[5] = (aux.contains("not") || aux.contains("n\'t")) ? "neg" : "";
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
	
	/*
	private static String addSlotLabel(String qlabel, String pp, String qval,
			CountDictionary slotDict) {
		if (qval == null || qval.isEmpty()) {
			return "";
		}
		String qkey = pp.isEmpty() ? qlabel : qlabel + "_" + pp;
		if (slotDict != null) {
			slotDict.addString(qkey + "=" + qval);
		}
		return pp.isEmpty() ? qlabel : qlabel + "_PP";
	}
	*/
	/*
	public static void encode(
			Sentence sentence, int propHead, ArrayList<QAPair> qaList,
			CountDictionary slotDict, CountDictionary tempDict) {	
		for (QAPair qa : qaList) {
			String[] question = qa.questionWords;
			assert (question.length == 7);
			String wh  = question[0],
				   aux = question[1],
				   ph1 = question[2],
				   trg = question[3],
				   ph2 = question[4],
				   pp  = question[5],
				   ph3 = question[6];
			boolean nullPh1 = ph1.isEmpty(),
					nullPh2 = ph2.isEmpty(),
					nullPh3 = ph3.isEmpty(),
					nullPP = pp.isEmpty();
			boolean verbalPh3 = (ph3.equals("do") || ph3.equals("doing") ||
								 ph3.equals("be") || ph3.equals("being"));
			boolean passiveVoice = isPassiveVoice(aux, trg),
					activeVoice = !passiveVoice;
			String whSlot = wh.equals("who") ? "someone" : "something",
				   whSlot2 = verbalPh3 ? ph3 + " something" : whSlot;
			
			String[] labels = new String[6];
			Arrays.fill(labels, "");
			labels[0] = wh;
			// Template format [wh, ph1, ph2, ph3, voice]
			String[] slots = new String[5];
			Arrays.fill(slots, "");

			if (isWhoWhat(wh)) {
				if (activeVoice && nullPh1) {
					// WH->ARG[0], PH2->ARG[1], PH3->ARG[2/pp]
					slots[0] = addSlotLabel("W0", "", whSlot, slotDict);
					slots[2] = addSlotLabel("W1", "", ph2, slotDict);
				} else if (activeVoice && !nullPh1 && nullPh2) {
					// WH->ARG[1], PH1->ARG[0], PH3->ARG[2/pp]
					slots[0] = addSlotLabel("W1", "", whSlot, slotDict);
					slots[1] = addSlotLabel("W0", "", ph1, slotDict);
				} else if (activeVoice && !nullPh1 && !nullPh2) {
					// WH->ARG[2/pp], PH1->ARG[0], PH2->ARG[1]
					slots[0] = addSlotLabel("W2", pp, whSlot2, slotDict);
					slots[1] = addSlotLabel("W0", "", ph1, slotDict);
					slots[2] = addSlotLabel("W1", "", ph2, slotDict);
				} else if (passiveVoice && nullPh1) {
					// WH->ARG[1], PH2->ARG[2]
					slots[0] = addSlotLabel("W1", "", whSlot, slotDict);
					slots[2] = addSlotLabel("W2", "", ph2, slotDict);
				} else if (passiveVoice && !nullPh1) {
					// WH->ARG[2/pp], PH1->ARG[1]
					slots[0] = addSlotLabel("W2", pp, whSlot2, slotDict);
					slots[1] = addSlotLabel("W1", "", ph1, slotDict);					
				}
				if (ph3.equals("somewhere")) {
					slots[3] = addSlotLabel("WHERE", pp, ".", slotDict);
				} else if (passiveVoice && pp.equals("by") &&
						(ph3.equals("someone") || ph3.equals("something"))) {
					slots[3] = addSlotLabel("W0", "", ph3, slotDict);
				} else if (!verbalPh3){
					slots[3] = addSlotLabel("W2", pp, ph3, slotDict);
				}
			} else {
				slots[0] = addSlotLabel(wh.toUpperCase(), "", ".", slotDict);
				if (!nullPP && nullPh3) {
					slots[0] = addSlotLabel(wh.toUpperCase(), pp, ".", slotDict);
				}
				if (activeVoice) {
					// PH1->ARG[0], PH2->ARG[1], PH3->ARG[2]
					slots[1] = addSlotLabel("W0", "", ph1, slotDict);
					slots[2] = addSlotLabel("W1", "", ph2, slotDict);
					slots[3] = addSlotLabel("W2", pp, ph3, slotDict);
				} else {
					// PH1->ARG[1], PH2->ARG[2]
					slots[1] = addSlotLabel("W1", "", ph1, slotDict);
					slots[2] = addSlotLabel("W2", "", ph2, slotDict);
					slots[3] = addSlotLabel("W2", pp, ph3, slotDict);
				}
			}
			slots[4] = (activeVoice ? "active" : "passive");
			String tempStr = StringUtils.join("\t", "_", slots);
			if (tempDict != null) {
				tempDict.addString(tempStr);
			}
		}
	}
	*/
	private static String getLabel(String qlabel, String pp, String qval) {
		if (qval == null || qval.isEmpty()) {
			return "";
		}
		String label = (pp.isEmpty() ? qlabel : qlabel + "_" + pp);
		return label + "=" + qval;
	}
	
	public static String[] getLabels(String[] question) {	
		assert (question.length == 7);
		String wh  = question[0],
			   aux = question[1],
			   ph1 = question[2],
			   trg = question[3],
			   ph2 = question[4],
			   pp  = question[5],
			   ph3 = question[6];
		boolean nullPh1 = ph1.isEmpty(),
				nullPh2 = ph2.isEmpty(),
				nullPh3 = ph3.isEmpty(),
				nullPP = pp.isEmpty();
		boolean verbalPh3 = (ph3.equals("do") || ph3.equals("doing") ||
							 ph3.equals("be") || ph3.equals("being"));
		boolean passiveVoice = isPassiveVoice(aux, trg),
				activeVoice = !passiveVoice;
		String whSlot = wh.equals("who") ? "someone" : "something",
			   whSlot2 = verbalPh3 ? ph3 + " something" : whSlot;
		
		// Template format [wh, ph1, ph2, ph3, voice]
		String[] template = new String[5];
		Arrays.fill(template, "");

		if (isWhoWhat(wh)) {
			if (activeVoice && nullPh1) {
				template[0] = getLabel("W0", "", whSlot);
				template[2] = getLabel("W1", "", ph2);
			} else if (activeVoice && !nullPh1 && nullPh2 && nullPP) {
				template[0] = getLabel("W1", "", whSlot);
				template[1] = getLabel("W0", "", ph1);
			} else if (activeVoice && !nullPh1) {
				template[0] = getLabel("W2", pp, whSlot2);
				template[1] = getLabel("W0", "", ph1);
				template[2] = getLabel("W1", "", ph2);
			} else if (passiveVoice && nullPh1) {
				template[0] = getLabel("W1", "", whSlot);
				template[2] = getLabel("W2", "", ph2);
			} else if (passiveVoice && !nullPh1) {
				template[0] = getLabel("W2", pp, whSlot2);
				template[1] = getLabel("W1", "", ph1);
			}
			if (ph3.equals("somewhere")) {
				template[3] = getLabel("WHERE", pp, ".");
			} else if (passiveVoice && pp.equals("by") &&
					(ph3.equals("someone") || ph3.equals("something"))) {
				template[3] = getLabel("W0", "", ph3);
			} else if (!verbalPh3){
				template[3] = getLabel("W2", pp, ph3);
			}
		} else {
			template[0] = getLabel(wh.toUpperCase(), "", ".");
			if (!nullPP && nullPh3) {
				template[0] = getLabel(wh.toUpperCase(), pp, ".");
			}
			if (activeVoice) {
				template[1] = getLabel("W0", "", ph1);
				template[2] = getLabel("W1", "", ph2);
				template[3] = getLabel("W2", pp, ph3);
			} else {
				template[1] = getLabel("W1", "", ph1);
				template[2] = getLabel("W2", "", ph2);
				template[3] = getLabel("W2", pp, ph3);
			}
		}
		template[4] = (activeVoice ? "active" : "passive");
		return template;
	}
	
	public static String getQuestionLabel(String[] question) {	
		assert (question.length == 7);
		String wh  = question[0],
			   aux = question[1],
			   ph1 = question[2],
			   trg = question[3],
			   ph2 = question[4],
			   pp  = question[5],
			   ph3 = question[6];
		boolean nullPh1 = ph1.isEmpty(),
				nullPh2 = ph2.isEmpty();
		boolean verbalPh3 = (ph3.equals("do") || ph3.equals("doing") ||
							 ph3.equals("be") || ph3.equals("being"));
		boolean passiveVoice = isPassiveVoice(aux, trg),
				activeVoice = !passiveVoice;
		String whSlot = wh.equals("who") ? "someone" : "something",
			   whSlot2 = verbalPh3 ? ph3 + " something" : whSlot;
		
		if (isWhoWhat(wh)) {
			if (activeVoice && nullPh1) {
				return getLabel("W0", "", whSlot);
			}
			if (activeVoice && !nullPh1 && nullPh2) {
				return getLabel("W1", "", whSlot);
			}
			if (activeVoice && !nullPh1 && !nullPh2) {
				return getLabel("W2", pp, whSlot2);
			}
			if (passiveVoice && nullPh1) {
				return getLabel("W1", "", whSlot);
			}
			return getLabel("W2", pp, whSlot2);
		}
		return getLabel(wh.toUpperCase(), "", ".");
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
