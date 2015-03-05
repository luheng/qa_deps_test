package data;

import java.util.ArrayList;
import java.util.Arrays;

import util.LatticeUtils;
import util.StringUtils;
import annotation.AnswerSpanAligner;
import annotation.CrowdFlowerQAResult;
import annotation.QuestionEncoder;

/**
 * This class contains
 * @author luheng
 *
 */
public class StructuredQAPair {
	public SRLSentence sentence;
	public int propHead;
	public String[] questionWords;
	public String questionLabel;
	public int[] answerFlags;
	public ArrayList<CrowdFlowerQAResult> cfAnnotationSources;
	
	public StructuredQAPair(SRLSentence sent, int prop, String[] question,
							String answer, CrowdFlowerQAResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = new String[question.length];
		for (int i = 0; i < question.length; i++) {
			questionWords[i] = question[i].toLowerCase();
		}
		questionLabel = QuestionEncoder.encode(questionWords, sentence);
		answerFlags = new int[sent.length];
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		cfAnnotationSources = new ArrayList<CrowdFlowerQAResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	public StructuredQAPair(SRLSentence sent, int prop, String qlabel,
			String answer, CrowdFlowerQAResult cf) { 
		sentence = sent;
		propHead = prop;
		questionWords = null;
		questionLabel = qlabel;
		answerFlags = new int[sent.length];
		Arrays.fill(answerFlags, 0);
		addAnswer(answer);
		cfAnnotationSources = new ArrayList<CrowdFlowerQAResult>();
		if (cf != null) {
			cfAnnotationSources.add(cf);
		}
	}
	
	public void addAnswer(String answer) {
		if (answer.isEmpty()) {
			return;
		}
		int[][] answerSpans = AnswerSpanAligner.align(sentence, answer);
		for (int[] span : answerSpans) {
			for (int i = span[0]; i < span[1]; i++) {
				answerFlags[i] = 1;
			}
		}
	}
	
	public void addAnswer(int[] flags) {
		for (int i = 0; i < answerFlags.length; i++) {
			answerFlags[i] = (flags[i] > 0 ? 1 : answerFlags[i]);
		}
	}
	
	public void addAnnotationSource(CrowdFlowerQAResult cfSource) {
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
	
	public String toString() {
		String result = questionLabel;
		//StringUtils.join(" ", questionWords);
		result += "\t [A]: " + getAnswerString();
		return result;
	}
}
