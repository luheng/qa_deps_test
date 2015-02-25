package data;

import java.util.ArrayList;

import util.StringUtils;
import annotation.AnswerSpanAligner;
import annotation.CrowdFlowerQAResult;

/**
 * This class contains
 * @author luheng
 *
 */
public class StructuredQAPair {
	// This is the most strict setting ..
	public static boolean whoEqualsWhom = false;
	public static boolean comparePPforEquality = true;
	public static boolean comparePassiveforEquality = true;
	
	public SRLSentence sentence;
	public int propositionId;
	public String whWord, ppWord;
	public String[] questionWords;
	public int[][] answerSpans;
	public boolean isPassive;
	public ArrayList<CrowdFlowerQAResult> cfAnnotationSources;
	
	public StructuredQAPair(SRLSentence sent, int prop, String[] question,
							String answer, CrowdFlowerQAResult cf) { 
		this.sentence = sent;
		this.propositionId = prop;
		
		this.questionWords = new String[question.length];
		for (int i = 0; i < question.length; i++) {
			this.questionWords[i] = question[i].toLowerCase();
		}
		this.whWord = question[0].toLowerCase();
		this.ppWord = question[5].toLowerCase();
		// TODO: identify passive voice ...
		this.isPassive = false;
		this.answerSpans = AnswerSpanAligner.align(sent, answer);
		this.cfAnnotationSources = new ArrayList<CrowdFlowerQAResult>();
		if (cf != null) {
			this.cfAnnotationSources.add(cf);
		}
	}
	
	public StructuredQAPair(SRLSentence sent, int prop, String[] question,
			int[][] answerSpans, CrowdFlowerQAResult cf) { 
		this.sentence = sent;
		this.propositionId = prop;
		this.questionWords = new String[question.length];
		for (int i = 0; i < question.length; i++) {
			this.questionWords[i] = question[i].toLowerCase();
		}
		this.whWord = question[0].toLowerCase();
		this.ppWord = question[5].toLowerCase();
		this.isPassive = false;
		this.answerSpans = answerSpans;
		this.cfAnnotationSources = new ArrayList<CrowdFlowerQAResult>();
		if (cf != null) {
			this.cfAnnotationSources.add(cf);
		}
	}
	
	private static boolean whWordEquals(String wh1, String wh2) {
		if (wh1.equals(wh2)) {
			return true;
		}
		return whoEqualsWhom && wh1.startsWith("who") &&
				wh2.startsWith("who");
	}
	
	private static boolean spansEquals(int[][] spans1, int[][] spans2) {
		if (spans1.length != spans2.length) {
			return false;
		}
		for (int i = 0; i < spans1.length; i++) {
			if (spans1[i][0] != spans2[i][0] || spans1[i][1] != spans2[i][1]) {
				return false;
			}
		}
		return true;
	}
	
	public String getNaiveQuestionEncoding() {
		String encoding = "";
		for (int i = 0; i < questionWords.length; i++) {
			if (i > 0) {
				encoding += " ";
			}
			if (questionWords[i].isEmpty()) {
				encoding += "[/]";
			} else {
				encoding += questionWords[i];
			}
		}
		return encoding;
	}
	
	public static String[] decodeQuestion(String qstr) {
		String[] qs = qstr.split(" ");
		String[] qwords = new String[qs.length];
		for (int i = 0; i < qs.length; i++) {
			if (qs[i].equals("[/]")) {
				qwords[i] = "";
			} else {
				qwords[i] = qs[i];
			}
		}
		return qwords;
	}
	
	/*
	public boolean questionEquals(SRLSentence sent, int prop, String wh,
								  String pp, boolean passive) {
		return this.sentence.sentenceID == sent.sentenceID &&
			   this.propositionId ==  prop && whWordEquals(this.whWord, wh) &&
			   (!comparePPforEquality || this.ppWord.equals(pp)) &&
			   (!comparePassiveforEquality || this.isPassive == passive);
	}
	*/
	
	/*
	public boolean equals(SRLSentence sent, int prop, String wh,
						  String pp, boolean passive, int[][] spans) {
		return this.questionEquals(sent, prop, wh, pp, passive) &&
			   spansEquals(this.answerSpans, spans);
	}
	*/
	
	/*
	@Override
	public boolean equals(Object obj) {
		StructuredQAPair qa = (StructuredQAPair) obj;
		return this.equals(qa.sentence, qa.propositionId, qa.whWord, qa.ppWord,
				qa.isPassive, qa.answerSpans);
	}
	*/
	
	/*
	public boolean questionEquals(StructuredQAPair qa) {
		return this.questionEquals(qa.sentence, qa.propositionId, qa.whWord,
				qa.ppWord, qa.isPassive);
	}
	*/
	
	public void addAnnotationSource(CrowdFlowerQAResult cfSource) {
		this.cfAnnotationSources.add(cfSource);
	}
	
	public String getAnswerString() {
		String answerStr = "";
		for (int i = 0; i < answerSpans.length; i++) {
			if (i > 0) {
				answerStr += " ... ";
			}
			answerStr += sentence.getTokenString(answerSpans[i]);
		}
		return answerStr;
	}
	
	public String toString() {
		//String result = ppWord + " " + whWord  + " " +
		//		sentence.getTokenString(this.propositionId);
		String result = StringUtils.join(" ", questionWords);
		if (this.isPassive) {
			result += " [passive]";
		}
		result += "\t [A]: " + getAnswerString();
		return result;
	}
}
