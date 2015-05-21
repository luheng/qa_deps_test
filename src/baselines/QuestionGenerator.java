package baselines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import learning.QASample;
import annotation.AuxiliaryVerbIdentifier;
import annotation.QASlots;
import data.Corpus;
import data.CountDictionary;
import data.Sentence;
import data.VerbInflectionDictionary;
import experiments.ExperimentUtils;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class QuestionGenerator {

	VerbInflectionDictionary inflDict = null;
	CountDictionary labelDict = null, tempDict = null;
	
	public QuestionGenerator(Corpus corpus, CountDictionary slotDict,
			CountDictionary tempDict) {
		inflDict = ExperimentUtils.loadInflectionDictionary(corpus);		
		this.labelDict = slotDict;
		this.tempDict = tempDict;
	}
	
	public static void qgenTest(Sentence sentence, int propHead, QASample sample) {
		String verb = sentence.getTokenString(propHead);
		// Find auxiliary verb
		String aux = "";
		/*
		for (TypedDependency dep : sample.kBestParses.get(0)) {
			if (dep.gov().index() == propHead + 1 &&
				dep.reln().toString().equals("auxpass"))  {
				aux = sentence.getTokenString(dep.dep().index() - 1);
			}
		}
		*/
		for (int i = propHead - 1; i >= 0 && i >= propHead - 3; i--) {
			String tok = sentence.getTokenString(i);
			if (tok.equals("\'s")) {
				continue;
			}
			if (AuxiliaryVerbIdentifier.isAuxiliaryVerb(sentence, i) ||
				tok.equals("not") || tok.equals("n\'t")) {
				aux = tok + " " + aux;
			}
		}
		aux = aux.trim();
		//System.out.println(sentence.getTokensString());
		if (aux.length() > 0) {
			System.out.println(aux + " "  +verb);
		}
	}
	
	private static double getTemplateScore(String[] temp, String pfx,
			HashMap<String, String> nslots, HashMap<String, Double> nscores) {
		// Must match WH slot
		if (!temp[0].equals(pfx)) {
			return -1.0;
		}
		double score = 0.0;
		for (int i = 0; i <= 3; i++) {
			if (temp[i].equals("_")) {
				continue;
			}
			if (!nslots.containsKey(temp[i])) {
				return -1;
			}
			score += nscores.get(temp[i]);
		}
		return score;
	}
	
	private static String getLabelPrefix(String lb) {
		return lb.split("=")[0].split("_")[0] + (lb.contains("_") ? "_PP" : "");
	}
	
	public ArrayList<String[]> generateQuestions(Sentence sentence, int propHead,
			HashMap<String, Double> labels) {
		assert (tempDict != null);
		// Now truly, generate the questions.
		String verb = sentence.getTokenString(propHead);
		String[] infl = inflDict.getBestInflections(verb.toLowerCase());
		ArrayList<String[]> questions = new ArrayList<String[]>();
		
		HashMap<String, String> slots = new HashMap<String, String>();
		HashMap<String, Double> scores = new HashMap<String, Double>();
		HashMap<String, String> nslots = new HashMap<String, String>();
		HashMap<String, Double> nscores = new HashMap<String, Double>();
		
		for (String lb : labels.keySet()) {
			String pfx = lb.split("=")[0],
				   val = lb.split("=")[1],
				   npfx = getLabelPrefix(lb);
			double sc = labels.get(lb);
			if (!slots.containsKey(pfx) || scores.get(pfx) < sc) {
				slots.put(pfx, val);
				scores.put(pfx, sc);
			}
			if (!nslots.containsKey(npfx) || nscores.get(npfx) < sc) {
				nslots.put(npfx, lb);
				nscores.put(npfx, sc);
			}
		}
		for (String lb : labels.keySet()) {
			String pfx = lb.split("=")[0];
			String npfx = getLabelPrefix(lb);
			String[] bestTemp = null;
			double bestTempScore = -1.0;
			int bestTempFreq = 0;
			for (String tempStr : tempDict.getStrings()) {
				String[] temp = tempStr.split("\t");
				int freq = tempDict.getCount(tempStr);
				double score = getTemplateScore(temp, npfx, nslots, nscores);
				if (score > bestTempScore ||
					(score == bestTempScore && freq > bestTempFreq)) {
					bestTempScore = score;
					bestTempFreq = freq;
					bestTemp = tempStr.split("\t");
				}
			}
			if (bestTemp == null) {
				System.out.println("Unable to find matching template.");
				continue;
			}
			String[] question = new String[QASlots.numSlots];
			Arrays.fill(question, "");
			// WH
			String whSlot = lb.split("=")[1];
			if (!whSlot.equals(".")) {
				question[0] = whSlot.equals("someone") ? "who" : "what";
			} else {
				question[0] = pfx.toLowerCase();
			}
			// AUX+TRG
			if (bestTemp[4].equals("active")) {
				if (pfx.startsWith("W0")) {
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
			String ph3Key = "", ph2Key = "";
			// PH1
			if (!bestTemp[1].equals("_")) {
				question[QASlots.PH1SlotId] = slots.get(bestTemp[1]);
			}
			// PH2
			if (!bestTemp[2].equals("_")) {
				ph2Key = bestTemp[2].contains("PP") ?
						nslots.get(bestTemp[2]) : bestTemp[2];
				question[QASlots.PH2SlotId] = slots.get(ph2Key);
			}
			// PH3
			if (!bestTemp[3].equals("_")) {
				ph3Key = bestTemp[3].contains("PP") ?
						nslots.get(bestTemp[3]) : bestTemp[3];
				question[QASlots.PH3SlotId] = ph3Key.startsWith("WHERE") ?
						"somewhere" : slots.get(ph3Key);
			}
			// PP
			if (pfx.contains("_")) {
				question[5] = pfx.split("_")[1];
			} else if (ph2Key.contains("_")) {
				question[5] = ph2Key.split("_")[1];
			} else if (ph3Key.contains("_")) {
				question[5] = ph3Key.split("_")[1];
			}
			questions.add(question);
		}
		return questions;
	}
	
	/**
	 * This might look more data-driven.
	 * @param sentence
	 * @param propHead
	 * @param results
	 * @return
	 */
	public ArrayList<String[]> generateQuestionsOld2(
			Sentence sentence, int propHead, TIntDoubleHashMap predLabels) {
		assert (tempDict != null);
		HashMap<String, String> slotValue = new HashMap<String, String>();
		HashMap<String, Double> slotScore = new HashMap<String, Double>();
		for (int id : predLabels.keys()) {
			double score = predLabels.get(id);
			if (id >= labelDict.size()) {
				System.out.println("Unidentified label ID:\t" + id);
				continue;
			}
			String qlabel = labelDict.getString(id);
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
		String[] infl = inflDict.getBestInflections(verb.toLowerCase());
		ArrayList<String[]> questions = new ArrayList<String[]>();
		HashMap<String, String> slotKeys = new HashMap<String, String>();
		for (String qkey : slotValue.keySet()) {
			slotKeys.put(qkey.contains("_") ? qkey.split("_")[0] + "_PP" : qkey,
						 qkey);
		}
		
		// System.out.println(StringUtils.join("\t", slotValue.keySet().toArray()));
		// System.out.println(StringUtils.join("\t", slotKeys.keySet().toArray()));
		
		for (String qkey : slotValue.keySet()) {
			// Try to find matching template
			String[] bestTemp = null;
			int numCoresCovered = 0;
			for (String tempStr : tempDict.getStrings()) {
				// Compute template score.
				String[] temp = tempStr.split("\t");
				if (!temp[0].split("_")[0].equals(qkey.split("_")[0])) {
					continue;
				}
				// System.out.println(tempStr);
				boolean match = true;
				int numCores = qkey.startsWith("ARG") ? 1 : 0;
				for (int i = 1; i <= 3; i++) {
					if (!temp[i].equals("_") && !slotKeys.containsKey(temp[i])) {
						match = false;
					}
					if (temp[i].startsWith("ARG")) {
						++ numCores;
					}
				}
				if (match && numCores > numCoresCovered) {
					bestTemp = tempStr.split("\t");
					numCoresCovered = numCores;
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
	public ArrayList<String[]> generateQuestionsOld(
			Sentence sentence,
			int propHead,
			HashMap<Integer, HashMap<Integer, TIntDoubleHashMap>> results) {
		HashMap<String, String> slotValue = new HashMap<String, String>();
		HashMap<String, Double> slotScore = new HashMap<String, Double>();
		TIntDoubleHashMap slots = results.get(sentence.sentenceID).get(propHead);
		for (int id : slots.keys()) {
			double score = slots.get(id);
			if (id >= labelDict.size()) {
				continue;
			}
			String qlabel = labelDict.getString(id);
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
