package data;

public class QAAnnotation {
	public DepSentence sentence;
	public String question, answer, questionType;
	public int[] alignedQuestion, alignedAnswer; // List of word IDs.
	//public WHWord questionWord;
	
	public QAAnnotation(String question, String ansewr, int[] alignedQuestion,
			            int[] alignedAnswer, String questionType,
			            DepSentence sentence) {
		this.question = question;
		this.answer = ansewr;
		this.alignedQuestion = alignedQuestion;
		this.alignedAnswer = alignedAnswer;
		this.questionType = questionType;
		this.sentence = sentence;
	}
	
	@Override
	public String toString() {
		// TODO: pretty print annotation.
		return "";
	}
}
