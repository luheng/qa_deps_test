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
import util.StrUtils;

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
	
	private static String[] getAuxiliary(Sentence sentence, int propHead) {
		String aux = "";
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
		return aux.isEmpty() ? null : aux.split(" ");
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
		String[] aux = getAuxiliary(sentence, propHead);
		boolean isPassive = false;
		if (aux != null) {
			String auxStr = StrUtils.join(" ", aux);
			System.out.println(sentence.getTokensString() + "\n" + sentence.getTokenString(propHead));
			System.out.println(auxStr);			
		}
		
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
				nslots.put(npfx, pfx);
				nscores.put(npfx, sc);
			}
		}
		for (String lb : labels.keySet()) {
			String whKey = lb.split("=")[0];
			String whVal = lb.split("=")[1];
			String npfx = getLabelPrefix(lb);
			String[] bestTemp = null;
			double bestTempScore = 0.0;
			for (String tempStr : tempDict.getStrings()) {
				String[] temp = tempStr.split("\t");
				//int freq = tempDict.getCount(tempStr);
				double score = getTemplateScore(temp, npfx, nslots, nscores);
				if (score > bestTempScore) {
					bestTempScore = score;
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
			if (!whVal.equals(".")) {
				question[QASlots.WHSlotId] =
						whVal.equals("someone") ? "who" : "what";
			} else {
				question[QASlots.WHSlotId] = whKey.toLowerCase().split("_")[0];
			}
			// AUX+TRG
			if (bestTemp[4].equals("active")) {
				if (whKey.startsWith("W0")) {
					question[QASlots.AUXSlotId] =
							verb.endsWith("ing") ? "is" : "";
					question[QASlots.TRGSlotId] = verb;
				} else {
					question[QASlots.AUXSlotId] = "did";
					question[QASlots.TRGSlotId] = infl[0];
				}
			} else {
				question[QASlots.AUXSlotId] = "is";
				question[QASlots.TRGSlotId] =
						verb.endsWith("ing") ? "being " + infl[4] : infl[4];
			}
			String ph1Key = "", ph2Key = "", ph3Key = "";
			// PH1
			if (!bestTemp[1].equals("_")) {
				ph1Key = bestTemp[2].contains("PP") ?
						nslots.get(bestTemp[1]) : bestTemp[1];
				question[QASlots.PH1SlotId] = slots.get(ph1Key);
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
			// System.out.println(whKey + ", " + ph1Key + ", " + ph2Key + ", " + ph3Key);
			// PP
			if (whKey.contains("_")) {
				question[QASlots.PPSlotId] = whKey.split("_")[1];
			} else if (ph3Key.contains("_")) {
				question[QASlots.PPSlotId] = ph3Key.split("_")[1];
			}
			questions.add(question);
		}
		return questions;
	}
}
