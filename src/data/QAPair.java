package data;

import java.util.ArrayList;
import java.util.Arrays;

import util.StringUtils;
import annotation.AnswerSpanAligner;
import annotation.CrowdFlowerResult;
import annotation.QuestionEncoder;

/**
 * This class contains
 * @author luheng
 *
 */
public class QAPair {
	public SRLSentence sentence;
	public int propHead;
	public String[] questionWords;
	public String questionLabel, questionString;
	public int[] answerFlags;
	public ArrayList<CrowdFlowerResult> cfAnnotationSources;
	
	public QAPair(SRLSentence sent, int prop, String[] question,
							String answer, CrowdFlowerResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = new String[question.length];
		for (int i = 0; i < question.length; i++) {
			questionWords[i] = question[i].toLowerCase();
		}
		questionLabel = QuestionEncoder.encode(questionWords, sentence);
		questionString = StringUtils.join(" ", questionWords);
		answerFlags = new int[sent.length];
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		cfAnnotationSources = new ArrayList<CrowdFlowerResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	public QAPair(SRLSentence sent, int prop, String qstr,
			String answer, CrowdFlowerResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = null;
		if (qstr.contains(" ")) {
			questionString = qstr;
			questionLabel = "";
		} else {
			questionString = "";
			questionLabel = qstr;
		}
		answerFlags = new int[sent.length];
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		cfAnnotationSources = new ArrayList<CrowdFlowerResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	public void addAnswer(String answer) {
		if (answer.isEmpty()) {
			return;
		}
		int[] matched = AnswerSpanAligner.align(sentence, answer);
		for (int i = 0; i < sentence.length; i++) {
			answerFlags[i] = matched[i];
		}
	}
	
	public void addAnswer(int[] flags) {
		for (int i = 0; i < answerFlags.length; i++) {
			answerFlags[i] = (flags[i] > 0 ? 1 : answerFlags[i]);
		}
	}
	
	public void addAnnotationSource(CrowdFlowerResult cfSource) {
		this.cfAnnotationSources.add(cfSource);
	}
	
	public String getQuestionLabel() {
		return questionLabel;
	}
	
	public String getAnswerString() {
		String answerStr = "";
		int prevIdx = -1;
		for (int i = 0; i < sentence.length; i++) {
			if (answerFlags[i] > 0) {
				answerStr +=  (i > prevIdx + 1 && prevIdx >= 0) ? " ... " : " ";
				answerStr += sentence.getTokenString(i);
				prevIdx = i;
			}
		}
		return answerStr;
	}
	
	public String getQuestionString() {
		if (questionWords == null) {
			return "";
		}
		return StringUtils.join(" ", questionWords) + "?";
	}
	
	public String toString() {
		String result = questionLabel;
		//StringUtils.join(" ", questionWords);
		result += "\t [A]: " + getAnswerString();
		return result;
	}
}
