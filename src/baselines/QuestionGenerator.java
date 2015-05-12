package baselines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import annotation.QASlots;
import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QuestionGenerator {

	VerbInflectionDictionary inflDict = null;
	CountDictionary slotDict = null, tempDict = null;
	
	public QuestionGenerator(Corpus corpus, CountDictionary qdict, CountDictionary tempDict) {
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);		
		this.slotDict = qdict;
		this.tempDict = tempDict;
	}
	
	/**
	 * This might look more data-driven.
	 * @param sentence
	 * @param propHead
	 * @param results
	 * @return
	 */
	public ArrayList<String[]> generateQuestions2(
			Sentence sentence,
			int propHead,
			HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results) {
		assert (tempDict != null);
		HashMap<String, String> slotValue = new HashMap<String, String>();
		HashMap<String, Double> slotScore = new HashMap<String, Double>();
		TIntDoubleHashMap slots = results.get(sentence.sentenceID).get(propHead);
		for (int id : slots.keys()) {
			double score = slots.get(id);
			if (id >= slotDict.size()) {
				continue;
			}
			String qlabel = slotDict.getString(id);
			String[] qinfo = qlabel.split("=");
			String qkey = qinfo[0], qval = qinfo[1];
			if (!slotScore.containsKey(qkey) || slotScore.get(qkey) < score) {
				slotValue.put(qkey, qval);
				slotScore.put(qkey, score);
			}
	
		}
		
		// Now truly, generate the questions.
		// 1. Subj questions
		String verb = sentence.getTokenString(propHead);
		System.out.println(verb);
		String[] infl = inflDict.getBestInflections(verb.toLowerCase());
		ArrayList<String[]> questions = new ArrayList<String[]>();
	
		HashMap<String, String> slotKeys = new HashMap<String, String>();
		for (String qkey : slotValue.keySet()) {
			slotKeys.put(qkey.contains("_") ? qkey.split("_")[0] + "_PP" : qkey,
						 qkey);
		}
		for (String qkey : slotValue.keySet()) {
			// Try to find matching template
			String[] bestTemp = null;
			for (String tempStr : tempDict.getStrings()) {
				String[] temp = tempStr.split("\t");
				if (!temp[0].split("_")[0].equals(qkey.split("_")[0])) {
					continue;
				}
				boolean match = true;
				for (int i = 1; i < temp.length; i++) {
					if (!temp[i].equals("_") && !slotKeys.containsKey(temp[i])) {
						match = false;
					}
				}
				if (match) {
					bestTemp = tempStr.split("\t");
					break;
				}
			}
			if (bestTemp == null) {
				continue;
			}
			
			String whSlot = slotValue.get(qkey);
			String ph3Key = "", ph3Slot = "";
			if (!bestTemp[3].equals("_")) {
				ph3Key = bestTemp[3].contains("PP") ? slotKeys.get(bestTemp[3]) : bestTemp[3];
				ph3Slot = ph3Key.startsWith("WHERE") ? "somewhere" : slotValue.get(ph3Key.split("_")[0]);
			}
					
			String[] question = new String[QASlots.numSlots];
			Arrays.fill(question, "");
			// WH
			if (qkey.startsWith("ARG")) {
				question[0] = whSlot.equals("someone") ? "who" : "what";
			} else {
				question[0] = qkey.toLowerCase();
			}
			// AUX+TRG
			if (bestTemp[4].equals("active")) {
				if (qkey.startsWith("ARG0")) {
					question[1] = verb.endsWith("ing") ? "is" : "";
					question[3] = verb;
				} else {
					question[1] = "did";
					question[3] = infl[0];
				}
			} else {
				question[1] = "is";
				question[3] = verb.endsWith("ing") ? "being " + infl[4] : infl[4];
			}
			// PH1
			question[2] = bestTemp[1].equals("_") ? "" : slotValue.get(bestTemp[1]);
			// PH2
			question[4] = bestTemp[2].equals("_") ? "" : slotValue.get(bestTemp[2]);
			// PP
			if (qkey.contains("_")) {
				question[5] = qkey.split("_")[1];
			} else if (ph3Key.contains("_")) {
				question[5] = ph3Key.split("_")[1];
			}
			// PH3
			question[6] = ph3Slot;
			
			
			questions.add(question);
		}
		return questions;
	}
	
	@Deprecated
	public ArrayList<String[]> generateQuestions(
			Sentence sentence,
			int propHead,
			HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results) {
		HashMap<String, String> slotValue = new HashMap<String, String>();
		HashMap<String, Double> slotScore = new HashMap<String, Double>();
		TIntDoubleHashMap slots = results.get(sentence.sentenceID).get(propHead);
		for (int id : slots.keys()) {
			double score = slots.get(id);
			if (id >= slotDict.size()) {
				continue;
			}
			String qlabel = slotDict.getString(id);
			String[] qinfo = qlabel.split("_");
			String qkey, qval;
			if (qinfo[0].equals("M")) {
				qkey = qlabel;
				qval = "";
			} else if (qinfo.length == 2) {
				qkey = qinfo[0];
				qval = qinfo[1];
			} else {
				qkey = qinfo[0] + "_" + qinfo[1];
				qval = qinfo[2]; 
			}
			
			if (!slotScore.containsKey(qkey) || slotScore.get(qkey) < score) {
				slotValue.put(qkey, qval);
				slotScore.put(qkey, score);
			}
	
		}
		// TODO: figure out "normal form"
		/*
		if (slotValue.containsKey("O2_do")) {
			System.out.println(
					(slotValue.containsKey("S") ? slotValue.get("S") : "_") + "\t" +
					sent.getTokenString(propHead) + "\t" +
					(slotValue.containsKey("O1") ? slotValue.get("O1") : "_") + "\t" +
					"to do something"
			);
		} else {
			System.out.println(
					(slotValue.containsKey("S") ? slotValue.get("S") : "_") + "\t" +
					sent.getTokenString(propHead) + "\t" +
					(slotValue.containsKey("O1") ? slotValue.get("O1") : "_") + "\t" +
					(slotValue.containsKey("O2") ? slotValue.get("O2") : "_")
			);
		}
		for (String qkey : slotValue.keySet()) {
			System.out.println(qkey + "\t" + slotValue.get(qkey) + "\t" + slotScore.get(qkey));
		}*/
		
		// Now truly, generate the questions.
		// 1. Subj questions
		String verb = sentence.getTokenString(propHead);
		System.out.println(verb);
		String[] infl = inflDict.getBestInflections(verb.toLowerCase());
		ArrayList<String[]> questions = new ArrayList<String[]>();
		
		boolean hasSubj = slotValue.containsKey("S"),
				hasObj1 = slotValue.containsKey("O1");
		
		for (String qkey : slotValue.keySet()) {
			String[] question = new String[QASlots.numSlots];
			Arrays.fill(question, "");
			if (qkey.equals("S")) {
				question[0] = slotValue.get("S").equals("someone") ? "who" : "what";
				if (verb.endsWith("ing")) {
					question[1] = "is";
				}
				question[2] = "";
				question[3] = verb;
				if (hasObj1) {
					question[4] = slotValue.get("O1");
				}
				if (slotValue.containsKey("O2_do")) {
					question[5] = "to";
					question[6] = "do something";
				}
			} else if (qkey.equals("O1")) {
				question[0] = slotValue.get("O1").equals("someone") ? "who" : "what";
				if (hasSubj) {
					question[1] = "did";
					question[2] = slotValue.get("S");
					question[3] = infl[0];
				} else {
					question[1] = "is";
					question[2] = "";
					question[3] = verb.endsWith("ing") ? "being " + infl[4] : infl[4];
				}
				question[4] = "";
				if (slotValue.containsKey("O2_do")) {
					question[5] = "to";
					question[6] = "do something";
				} else {
					question[5] = "";
					question[6] = "";
				}
			} else if (qkey.equals("O2_do")) {
				question[0] = "what";
				question[1] = "is";
				question[2] = slotValue.get("O1");
				question[3] = infl[4];
				question[4] = "";
				question[5] = "to";
				question[6] = "do";
			} else if (qkey.startsWith("O2_")) {
				question[0] = slotValue.get(qkey).equals("someone") ? "who" : "what";
				question[1] = "is";
				question[2] = slotValue.get("O1");
				question[3] = infl[4];
				question[4] = "";
				question[5] = qkey.split("_")[1];
				question[6] = "";
			} else if (qkey.startsWith("M_")) {
				String[] qinfo = qkey.split("_");
				question[0] = qinfo[1];
				if (hasSubj) {
					question[1] = verb.endsWith("ing") ? "is" : "did";
					question[2] = slotValue.get("S");
					question[3] = verb.endsWith("ing") ? verb : infl[0];
				} else {
					question[1] = "is";
					question[2] = slotValue.get("O1");
					question[3] = verb.endsWith("ing") ? "being " + infl[4] : infl[4];
				}
				question[4] = "";
				if (qinfo.length > 2) {
					question[5] = qinfo[2];
					question[6] = "";
				} else if (slotValue.containsKey("O2_do")) {
					question[5] = "to";
					question[6] = "do something";
				} else {
					question[5] = "";
					question[6] = "";
				}
			} else {
				continue;
			}
			boolean hasNull = false;
			for (String q : question) {
				if (q == null) {
					hasNull = true;
				}
			}
			if (!hasNull) {
				questions.add(question);
			}
		}
		return questions;
	}
}
