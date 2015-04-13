package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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
	public Sentence sentence;
	public int propHead;
	public String[] questionWords;
	public String questionLabel, questionString;
	public int[] answerFlags;
	public ArrayList<String> answers;
	public ArrayList<CrowdFlowerResult> cfAnnotationSources;
	public String annotator;
	public String comment = "";
	
	public QAPair(Sentence sent, int prop, String[] question, String answer,
			CrowdFlowerResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = new String[question.length];
		for (int i = 0; i < question.length; i++) {
			questionWords[i] = question[i].toLowerCase();
		}
		questionLabel = QuestionEncoder.encode(questionWords, sentence);
		questionString = StringUtils.join(" ", questionWords);
		answerFlags = new int[sent.length];
		answers = new ArrayList<String>();
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		
		cfAnnotationSources = new ArrayList<CrowdFlowerResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	public QAPair(Sentence sent, int prop, String qstr, String answer,
			CrowdFlowerResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = null;
		if (qstr.contains(" ")) {
			questionString = qstr;
			questionLabel = "";
		} else {
			questionString = "";
			questionLabel = qstr.toLowerCase();
		}
		answerFlags = new int[sent.length];
		answers = new ArrayList<String>();
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		
		cfAnnotationSources = new ArrayList<CrowdFlowerResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	/** return false if answer is not aligned */
	public boolean addAnswer(String answer) {
		if (answer.isEmpty()) {
			return false;
		}
		answers.add(answer);
		int[] matched = AnswerSpanAligner.align(sentence, answer);
		boolean aligned = false;
		for (int i = 0; i < sentence.length; i++) {
			answerFlags[i] += matched[i];
			if (matched[i] > 0) {
				aligned = true;
			}
		}
		return aligned;
	}
	
	public void addAnswer(int[] flags) {
		for (int i = 0; i < answerFlags.length; i++) {
			//answerFlags[i] = (flags[i] > 0 ? 1 : answerFlags[i]);
			answerFlags[i] += flags[i];
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
		return answerStr.trim();
	}
	
	public String getQuestionString() {
		if (questionWords == null) {
			return "";
		}
		return StringUtils.join(" ", questionWords) + "?";
	}
	
	public String getPaddedQuestionString() {
		if (questionWords == null) {
			return "";
		}
		String qstr = "";
		for (String qw : questionWords) {
			qstr += (qw.isEmpty() ? "_" : qw.trim()) + "\t";
		}
		return qstr + "?";
	}
	
	public String toString() {
		String result = questionLabel;
		//StringUtils.join(" ", questionWords);
		result += "\t [A]: " + getAnswerString();
		return result;
	}
}
