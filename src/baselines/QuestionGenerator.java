package baselines;

import java.util.ArrayList;
import java.util.HashMap;

import annotation.QASlots;
import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QuestionGenerator {

	VerbInflectionDictionary inflDict = null;
	CountDictionary qdict = null;
	
	public QuestionGenerator(Corpus corpus, CountDictionary qdict) {
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);		
		this.qdict = qdict;
	}
	
	public ArrayList<String[]> generateQuestions(
			Sentence sentence,
			int propHead,
			HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results) {
		
		HashMap<String, String> slotValue = new HashMap<String, String>();
		HashMap<String, Double> slotScore = new HashMap<String, Double>();
		TIntDoubleHashMap slots = results.get(sentence.sentenceID).get(propHead);
		
		for (int id : slots.keys()) {
			double score = slots.get(id);
			if (id >= qdict.size() ||
				score < 0.1) {
				continue;
			}
			String qlabel = qdict.getString(id);
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
		int inflId = inflDict.getBestInflectionId(verb);
		String[] infl = inflDict.inflections.get(inflId);
		ArrayList<String[]> questions = new ArrayList<String[]>();
		
		boolean hasSubj = slotValue.containsKey("S"),
				hasObj1 = slotValue.containsKey("O1");
		
		for (String qkey : slotValue.keySet()) {
			String[] question = new String[QASlots.numSlots];
			for (int i = 0; i < QASlots.numSlots; i++) {
				question[i] = "";
			}
			if (qkey.equals("S")) {
				question[0] = slotValue.get("S").equals("someone") ? "who" : "what";
				question[1] = "";
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
					question[3] = infl[4];
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
				question[1] = "is";
				question[2] = slotValue.get("O1");
				question[3] = infl[4];
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
			questions.add(question);
		}
		return questions;
	}
}
