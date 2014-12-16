package ui;

import java.awt.Choice;
import java.util.ArrayList;

public class QuestionWordChooser extends Choice {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public QuestionWordChooser() {
		this.add("Who");
		this.add("What");
		this.add("Where");
		this.add("When");
		this.add("How");
		this.add("Why");
	}
	
	public QuestionWordChooser(ArrayList<String> qwords) {
		for (String qword : qwords) {
			this.add(qword);
		}
	}
}
