package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JTextArea;

import util.DataUtils;
import util.DebugUtils;
import annotation.CandidateProposition;
import data.AnnotatedDepSentence;
import data.DepSentence;
import data.QAPairOld;
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
	JTextArea sentDisplay, verbInput, answerInput, qaDisplay;
	Label questionLabel, answerLabel, questionMarkLabel; 
	Button saveQAButton, prevPropButton, nextPropButton, prevSentButton,
		   nextSentButton;
	
	ArrayList<AnnotatedDepSentence> annotatedSentences; 
	ArrayList<CandidateProposition> currentPropositions;
	int currentPropositionId, currentSentenceId;
	
	public AnnotatorFrame(ArrayList<DepSentence> sentences) {
		super("Question-Answer Annotator");
		setSize(800, 360);
		addWindowListener(new AnnotatorWindowMonitor());
	
	    Panel panel = new Panel();
	    GroupLayout layout = new GroupLayout(panel);
	    panel.setLayout(layout);
	    layout.setAutoCreateGaps(true);
	    layout.setAutoCreateContainerGaps(true);
	    
	    Font font = new Font("TimesRoman", Font.PLAIN, 20);
	    
	    // Show sentence.
	    sentDisplay = new JTextArea(3, 100);
	    sentDisplay.setEditable(false);
	    sentDisplay.setLineWrap(true);
	    sentDisplay.setFont(font);
	  
	    questionLabel = new Label("Q:");
	    questionLabel.setFont(font);
	    
	    whChooser = new QuestionWordChooser();
	    
	    verbInput = new JTextArea(1, 50);	    
	    verbInput.setEditable(true);
	    verbInput.setFont(font);
	    
	    pronChooser1 = new PronounChooser();
	    pronChooser2 = new PronounChooser();
	    
	    questionMarkLabel = new Label("?");
	    questionMarkLabel.setFont(font);
	    
	    answerLabel = new Label("A:");
	    answerInput = new JTextArea(1,200);
	    answerInput.setEditable(true);
	    answerInput.setFont(font);
	    
	    saveQAButton = new Button("Save QA");
	    prevPropButton = new Button("Prev Proposition");
	    nextPropButton = new Button("Next Proposition");
	    prevSentButton = new Button("Prev Sentence");
	    nextSentButton = new Button("Next Sentence");
	    
	    saveQAButton.addActionListener(this);
	    prevPropButton.addActionListener(this);
	    nextPropButton.addActionListener(this);
	    prevSentButton.addActionListener(this);
	    nextSentButton.addActionListener(this);
	    
	    qaDisplay = new JTextArea(5,100);
	    qaDisplay.setEditable(false);
	    qaDisplay.setLineWrap(false);
	    
	    // Add everything.
	    panel.add(sentDisplay);
	    panel.add(whChooser);
	    panel.add(verbInput);
	    panel.add(pronChooser1);
	    panel.add(pronChooser2);
	    panel.add(questionMarkLabel);
	    panel.add(saveQAButton);
	    panel.add(prevPropButton);
	    panel.add(nextPropButton);
	    panel.add(prevSentButton);
	    panel.add(nextSentButton);
	    panel.add(qaDisplay);
	    
	    Group horizontalQuestionGroup = layout.createSequentialGroup()
	    		.addComponent(questionLabel)
				.addComponent(whChooser)
				.addComponent(verbInput)
				.addComponent(pronChooser1)
				.addComponent(pronChooser2)
				.addComponent(questionMarkLabel);
	    
	    Group vertialQuestionGroup = layout.createParallelGroup(
	    			GroupLayout.Alignment.CENTER)
	    		.addComponent(questionLabel)
				.addComponent(whChooser)
				.addComponent(verbInput)
				.addComponent(pronChooser1)
				.addComponent(pronChooser2)
				.addComponent(questionMarkLabel);
	    
	    Group horizontalButtonGroup = layout.createSequentialGroup()
	    		.addComponent(saveQAButton)
	    		.addComponent(prevPropButton)
				.addComponent(nextPropButton)
				.addComponent(prevSentButton)
				.addComponent(nextSentButton);
	    
	    Group verticalButtonGroup = layout.createParallelGroup()
	    		.addComponent(saveQAButton)
	    		.addComponent(prevPropButton)
				.addComponent(nextPropButton)
				.addComponent(prevSentButton)
				.addComponent(nextSentButton);
	    
	    layout.setHorizontalGroup(layout.createParallelGroup()
	    		.addComponent(sentDisplay)
	    		.addGroup(horizontalQuestionGroup)
	    		.addGroup(layout.createSequentialGroup()
	    				.addComponent(answerLabel)
	    				.addComponent(answerInput))
	    		.addGroup(horizontalButtonGroup)
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
	    		.addGroup(verticalButtonGroup)
	    		.addComponent(qaDisplay));
	    
	    add(panel, BorderLayout.CENTER);
	    
		// Data initialization.
		initializeData(sentences);
	    refreshPanel();
	}
	
	private void refreshPanel() {
		AnnotatedDepSentence annotatedSentence =
				annotatedSentences.get(currentSentenceId);
		SRLSentence sentence = (SRLSentence) annotatedSentence.depSentence;
		
		String sentDisplayString = String.format("(%d) %s", currentSentenceId,
				sentence.getTokensString());
		sentDisplay.setText(sentDisplayString);
		verbInput.setText(currentPropositions.get(currentPropositionId)
				.getPropositionString());
		answerInput.setText("");
		
		if (currentPropositionId == 0) {
			prevPropButton.setEnabled(false);
		} else {
			prevPropButton.setEnabled(true);
		}
		if (currentPropositionId == currentPropositions.size() - 1) {
			nextPropButton.setEnabled(false);
		} else {
			nextPropButton.setEnabled(true);
		}
		if (currentSentenceId == 0) {
			prevSentButton.setEnabled(false);
		} else {
			prevSentButton.setEnabled(true);
		}
		if (currentSentenceId == annotatedSentences.size() - 1) {
			nextSentButton.setEnabled(false);
		} else {
			nextSentButton.setEnabled(true);
		}
		// Show current QAs
		String qaText = "";
		for (QAPairOld qa : annotatedSentence.qaList) {
			qaText += qa.toString() + "\n";
			
		}
		qaDisplay.setText(qaText);
		
		// Save everything.
		String corpusName =
				annotatedSentences.get(0).depSentence.corpus.corpusName;
		try {
			DataUtils.saveAnnotatedSentences(annotatedSentences,
					String.format("%s.qa.tmp", corpusName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeData(ArrayList<DepSentence> sentences) {
		annotatedSentences = new ArrayList<AnnotatedDepSentence>();
		for (DepSentence sentence : sentences) {
			annotatedSentences.add(new AnnotatedDepSentence(sentence));
		}
		currentPropositions = getPropositions();
		currentSentenceId = 0;
		currentPropositionId = 0;
	}
	
	private ArrayList<CandidateProposition> getPropositions() {
		SRLSentence sentence = (SRLSentence) annotatedSentences
				.get(currentSentenceId).depSentence;
		ArrayList<CandidateProposition> props =
				InteractiveAnnotationExperiment.getCandidatePropositions(
						sentence, true /* verb only */);
		
		// Print Gold;
		String[][] semGold = sentence.getSemanticArcs();
		DebugUtils.printSemanticArcs(sentence, semGold);
		
		return props;
	}

	private String getQuestion() {
		return whChooser.getSelectedItem().trim() + " " +
				verbInput.getText().trim() + " " +
				pronChooser1.getSelectedItem().trim() + " " +
				pronChooser2.getSelectedItem().trim() + " " + "?";
	}
	
	private String getAnswer() {
		// TODO: validate answer to be a contiguous span.
		return answerInput.getText().trim();
	}
	
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		if (command.equals("Save QA")) {
			String question = getQuestion();
			String answer = getAnswer();
			// System.out.println(question + ", " + answer);
			QAPairOld qa = new QAPairOld(question, answer);
			annotatedSentences.get(currentSentenceId).addQA(qa);
			refreshPanel();
			
		} else if (command.equals("Prev Proposition")) {
			currentPropositionId --;
			refreshPanel();
		} else if (command.equals("Next Proposition")) {
			currentPropositionId ++;
			refreshPanel();
		} else if (command.equals("Prev Sentence")) {
			currentSentenceId --;
			currentPropositions = getPropositions();
			refreshPanel();
		} else if (command.equals("Next Sentence")) {
			currentSentenceId ++;
			currentPropositions = getPropositions();
			refreshPanel();
		}
	}

	  public static void main(String args[]) {
		  SRLCorpus corpus = ExperimentUtils.loadSRLCorpus("en-srl-train");
		  
		  ArrayList<DepSentence> sentences = new ArrayList<DepSentence>();
		  /*
		  for (DepSentence sentence : corpus.sentences) {
			  if (sentence.getTokensString().contains("expected to")) {
				  sentences.add(sentence);
			  }
		  }*/
		  AnnotatorFrame frame = new AnnotatorFrame(sentences);
		  frame.setVisible(true);
	  }
}