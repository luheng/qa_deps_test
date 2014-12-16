package ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.GroupLayout;

public class AnnotatorFrame extends Frame implements ActionListener {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	QuestionWordChooser whChooser;
	PropositionChooser verbChooser;
	PronounChooser pronChooser;
	
	Button submitButton;

	public AnnotatorFrame() {
		super("Question-Answer Annotator");
		setSize(600, 280);
		addWindowListener(new AnnotatorWindowMonitor());
  
	    Panel panel = new Panel();
	    //toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
	    GroupLayout layout = new GroupLayout(panel);
	    panel.setLayout(layout);
	    
	    whChooser = new QuestionWordChooser();
	    panel.add(whChooser);
	    
	    verbChooser = new PropositionChooser("breaks");
	    panel.add(verbChooser);
	    
	    pronChooser = new PronounChooser();
	    panel.add(pronChooser);
	    
	    submitButton = new Button("Submit");
	    submitButton.addActionListener(this);
	    panel.add(submitButton);
	
	    add(panel, BorderLayout.NORTH);
	}

	  public void actionPerformed(ActionEvent ae) {
		  System.out.println(ae.getActionCommand());
	  }

	  public static void main(String args[]) {
		    AnnotatorFrame frame = new AnnotatorFrame();
		    frame.setVisible(true);
	  }
}