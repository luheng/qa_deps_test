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
			if (tok.equals("to")) {
				// "have to"
				break;
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
	
	// active aux, active trg, passive aux passive trg
	private String[][] getAuxTrg(Sentence sentence, int propHead) {
		String[][] ss = new String[2][];
		
		String verb = sentence.getTokenString(propHead).toLowerCase();
		String[] infl = inflDict.getBestInflections(verb);
		
		String[] aux = getAuxiliary(sentence, propHead);
		boolean isPassive = false;
		String newStr = "";
		String auxStr = (aux == null ? "" : StrUtils.join(" ", aux));
		
		//System.out.println(auxStr + " " + sentence.getTokenString(propHead));

		if (!verb.endsWith("ing") && (
				auxStr.contains("been") || auxStr.contains("being") ||
				auxStr.contains("be") || auxStr.contains("is") ||
				auxStr.contains("are") || auxStr.contains("were") ||
				auxStr.contains("was"))) {
			isPassive = true;
		}
		if (auxStr.isEmpty()) {
			if (verb.equals(infl[0]) && !verb.equals(infl[3])) {
				auxStr = "would";
			} else if (verb.equals(infl[2])) {
				auxStr = "is";
			}
		}
		//System.out.println(hasNegation ? "Neg" : "");
		//System.out.println(isPassive ? "Passive" : "Active");
		if (isPassive) {
			// make active
			if (auxStr.contains("been")) {
				newStr = auxStr.replaceAll("been", "") + " " + verb;
			} else if (auxStr.contains("being")) {
				newStr = auxStr.replaceAll("being", "") + " " + infl[2];
			} else if (auxStr.contains("be")) {
				newStr = auxStr.replaceAll("be", "") + " " + infl[0];
			} else if (auxStr.contains("is") || auxStr.contains("are")) {
				newStr = infl[1];
			} else if (auxStr.contains("were") || auxStr.contains("was")) {
				newStr = infl[4];
			}
			// System.out.println("pas->act:\t" + newStr);
		} else {
			// make passive
			if (verb.equals(infl[2])) {
				newStr = auxStr + " being " + infl[4];
				if (auxStr.isEmpty() || auxStr.startsWith("not") || auxStr.startsWith("n\'t")) {
					newStr = "is " + newStr;
				}
			} else if (verb.equals(infl[4]) &&
					(auxStr.contains("has") || auxStr.contains("have") ||
					 auxStr.contains("had"))) {
				newStr = auxStr + " been " + infl[4];
			} else if (verb.equals(infl[1])) {
				if (auxStr.isEmpty()) {
					newStr = "is " + infl[4];
				} else if (auxStr.contains("does ")) {
					newStr = auxStr.replace("does", "is") + " " + infl[4];
				} else if (auxStr.contains("do")) {
					newStr = auxStr.replace("do", "is") + " " + infl[4];
				}  else if (auxStr.contains("did")) {
					newStr = auxStr.replace("did", "was") + " " + infl[4];
				} else {
					newStr = auxStr + " be " + infl[4];
				}
			} else {
				newStr = "was " + infl[4];
			}
			// System.out.println("act->pas:\t" + newStr);
		}
		auxStr = auxStr + " " + verb;
		if (isPassive) {
			ss[0] = newStr.trim().split(" ");
			ss[1] = auxStr.trim().split(" ");
		} else {
			ss[1] = newStr.trim().split(" ");
			ss[0] = auxStr.trim().split(" ");
		}
		return ss;
	}
	
	public ArrayList<String[]> generateQuestions(Sentence sentence, int propHead,
			HashMap<String, Double> labels) {
		assert (tempDict != null);
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
		
		String verb = sentence.getTokenString(propHead);
		String[] infl = inflDict.getBestInflections(verb.toLowerCase());
		String[][] ss = getAuxTrg(sentence, propHead);
		
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
				System.out.println(sentence.getTokensString() + "\n" + sentence.getTokenString(propHead));
				for (String l : labels.keySet()) {
					System.out.print(l + "\t");
				}
				System.out.println();
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
			int v = bestTemp[4].equals("active") ? 0 : 1;
			if (ss[v].length == 1) {
				if (bestTemp[1].equals("_")) {
					question[QASlots.TRGSlotId] = ss[v][0];
				} else {
					if (ss[v][0].equals(infl[3])) {
						question[QASlots.AUXSlotId] = "did";
					} else if (ss[v][0].equals(infl[1])) {
						question[QASlots.AUXSlotId] = "does";
					} else {
						question[QASlots.AUXSlotId] = "do";
					}
					question[QASlots.TRGSlotId] = infl[0];
				}
			} else if (ss[v].length > 2 && (ss[v][1].equals("n\'t") ||
											ss[v][1].equals("not"))) {
				question[QASlots.AUXSlotId] = ss[v][0] + " " + ss[v][1];
				question[QASlots.TRGSlotId] = StrUtils.join(" ", ss[v], 2);
			} else {
				question[QASlots.AUXSlotId] = ss[v][0];
				question[QASlots.TRGSlotId] = StrUtils.join(" ", ss[v], 1);
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
