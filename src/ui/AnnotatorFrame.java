package ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JTextArea;

import annotation.CandidateProposition;
import data.SRLCorpus;
import data.SRLSentence;
import experiments.ExperimentUtils;
import experiments.InteractiveAnnotationExperiment;

public class AnnotatorFrame extends Frame implements ActionListener {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	QuestionWordChooser whChooser;
	PropositionChooser verbChooser;
	PronounChooser pronChooser1, pronChooser2;
	JTextArea sentDisplay, verbInput, answerInput;
	Label questionLabel, answerLabel; 
	Button qsubButton, asubButton;
	
	public AnnotatorFrame(SRLSentence sentence,
						  ArrayList<CandidateProposition> propositions) {
		
		super("Question-Answer Annotator");
		
		setSize(600, 280);
		addWindowListener(new AnnotatorWindowMonitor());
  
	    Panel panel = new Panel();
	    //panel.setLayout(new FlowLayout(FlowLayout.LEFT));
	    
	    GroupLayout layout = new GroupLayout(panel);
	    panel.setLayout(layout);
	    layout.setAutoCreateGaps(true);
	    layout.setAutoCreateContainerGaps(true);
	    
	    // Show sentence.
	    sentDisplay = new JTextArea(sentence.getTokensString(), 3, 100);
	    sentDisplay.setEditable(false);
	    sentDisplay.setLineWrap(true);
	 
	    questionLabel = new Label("Q:");
	    whChooser = new QuestionWordChooser();

	    String verbString = propositions.get(0).getPropositionString();
	    verbInput = new JTextArea(verbString, 1, 50);	    
	    verbInput.setEditable(true);
	    
	    pronChooser1 = new PronounChooser();
	    pronChooser2 = new PronounChooser();
	    
	    answerLabel = new Label("A:");
	    answerInput = new JTextArea(1,200);
	    answerInput.setEditable(true);
	    
	    qsubButton = new Button("Submit Question");
	    asubButton = new Button("Submit Answer");
	    qsubButton.addActionListener(this);
	    asubButton.addActionListener(this);
	    
	    
	    JTextArea qaDisplay =
	    		new JTextArea(5,100);
	    qaDisplay.setEditable(false);
	    qaDisplay.setLineWrap(false);
	    
	    
	    // Add everything.
	    panel.add(sentDisplay);
	    panel.add(whChooser);
	    panel.add(verbInput);
	    panel.add(pronChooser1);
	    panel.add(pronChooser2);
	    panel.add(qsubButton);
	    panel.add(asubButton);
	    panel.add(qaDisplay);
	    
	    Group horizontalQuestionGroup = layout.createSequentialGroup()
	    		.addComponent(questionLabel)
				.addComponent(whChooser)
				.addComponent(verbInput)
				.addComponent(pronChooser1)
				.addComponent(pronChooser2);
	    
	    Group vertialQuestionGroup = layout.createParallelGroup()
	    		.addComponent(questionLabel)
				.addComponent(whChooser)
				.addComponent(verbInput)
				.addComponent(pronChooser1)
				.addComponent(pronChooser2);
	    
	    layout.setHorizontalGroup(layout.createParallelGroup()
	    		.addComponent(sentDisplay)
	    		.addGroup(horizontalQuestionGroup)
	    		.addGroup(layout.createSequentialGroup()
	    				.addComponent(answerLabel)
	    				.addComponent(answerInput))
	    		.addGroup(layout.createSequentialGroup()
	    				.addComponent(qsubButton)
	    				.addComponent(asubButton))
	    		.addComponent(qaDisplay));
	    		
	    layout.setVerticalGroup(layout.createSequentialGroup()
	    		.addComponent(sentDisplay)
	    		.addGroup(layout.createParallelGroup()
	    				.addGroup(layout.createSequentialGroup()
	    						.addComponent(questionLabel)
	    						.addComponent(answerLabel))
	    				.addGroup(layout.createSequentialGroup()
	    						.addGroup(vertialQuestionGroup)
	    						.addComponent(answerInput)))
	    		.addGroup(layout.createParallelGroup()
	    				.addComponent(qsubButton)
	    				.addComponent(asubButton))
	    		.addComponent(qaDisplay));
	    
	    add(panel, BorderLayout.CENTER);
	}

	  public void actionPerformed(ActionEvent ae) {
		  System.out.println(ae.getActionCommand());
	  }

	  public static void main(String args[]) {
		  SRLCorpus corpus = ExperimentUtils.loadSRLCorpus();
		  
		  SRLSentence sentence = (SRLSentence) corpus.sentences.get(0);
		  // Get a list of verbs.
		  ArrayList<CandidateProposition> props =
				  InteractiveAnnotationExperiment.getCandidatePropositions(
						  sentence, true /* verb only */);

		  AnnotatorFrame frame = new AnnotatorFrame(sentence, props);
		  
		  frame.setVisible(true);
	  }
}