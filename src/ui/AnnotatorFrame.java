package ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.GroupLayout;
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
	    JTextArea sentDisplay =
	    		new JTextArea(sentence.getTokensString(), 3, 100);
	    sentDisplay.setEditable(false);
	    sentDisplay.setLineWrap(true);
	 
	    panel.add(sentDisplay);
	    
	    whChooser = new QuestionWordChooser();
	    panel.add(whChooser);	    
	    
	    String verbString = propositions.get(0).getPropositionString();
	    verbChooser = new PropositionChooser(verbString);
	    panel.add(verbChooser);
	    
	    pronChooser1 = new PronounChooser();
	    panel.add(pronChooser1);
	    
	    pronChooser2 = new PronounChooser();
	    panel.add(pronChooser2);
	    
	    qsubButton = new Button("Submit Question");
	    asubButton = new Button("Submit Answer");
	    qsubButton.addActionListener(this);
	    asubButton.addActionListener(this);
	    panel.add(qsubButton);
	    panel.add(asubButton);
	    
	    JTextArea qaDisplay =
	    		new JTextArea(5,100);
	    qaDisplay.setEditable(false);
	    qaDisplay.setLineWrap(false);
	    panel.add(qaDisplay);
	    
	    layout.setHorizontalGroup(layout.createParallelGroup()
	    		.addComponent(sentDisplay)
	    		.addGroup(layout.createSequentialGroup()
	    				.addComponent(whChooser)
	    				.addComponent(verbChooser)
	    				.addComponent(pronChooser1)
	    				.addComponent(pronChooser2))	    	
	    		.addGroup(layout.createSequentialGroup()
	    				.addComponent(qsubButton)
	    				.addComponent(asubButton))
	    		.addComponent(qaDisplay));
	    		
	    layout.setVerticalGroup(layout.createSequentialGroup()
	    		.addComponent(sentDisplay)
	    		.addGroup(layout.createParallelGroup(
	    				GroupLayout.Alignment.BASELINE)
	    				.addComponent(whChooser)
	    				.addComponent(verbChooser)
	    				.addComponent(pronChooser1)
	    				.addComponent(pronChooser2))
	    		.addGroup(layout.createParallelGroup(
	    				GroupLayout.Alignment.BASELINE)
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