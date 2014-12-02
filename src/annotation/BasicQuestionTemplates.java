package annotation;

import java.util.ArrayList;


/**
 * Templates for generating questions in an automated/semi-automated way.
 * 
 * Example templates:
 * 
 * <Who, VERB>	Who VERB that ?	Who concedes that ?
   <VERB, What>	What did X VERB ?	What did X concede ?
   <How/When/Where VERB>	How/When/Where did X VERB that ?	How did X concede?
   <How, VERB=be>	How is X ?	
   <What kind of, NOUN>	What kind of NOUN is mentioned?	What kind of growth is mentioned?
   <How much/many, NOUN>	How much/many is the NOUN?	How much is the takeover? 
   <ADP=Since, When>	Since when?	Since when?
   <What, VERB=be>	What be X?	What has n't been dramatic?

 * @author luheng
 *
 */

public class BasicQuestionTemplates {

	public ArrayList<QuestionTemplate> templates;
	
	public BasicQuestionTemplates() {
		templates = new ArrayList<QuestionTemplate>();
		templates.add(new QuestionTemplate("VERB", "", "who/what", 0,
										   "Who/what ### ?", true, false));
		
		templates.add(
				new QuestionTemplate("VERB", "", "what", 1, "### what ?",
									 false, true));
		
		// TODO: also, we might be expecting an Adverb here, specifically.
		templates.add(
				new QuestionTemplate("VERB", "", "how", 0,
									 "How did X ### ?", false, false));
		
		templates.add(
				new QuestionTemplate("NOUN", "", "what", 0, "What ### ?",
									 true, false));
		templates.add(
				new QuestionTemplate("ADJ", "", "how", 0, "How ### ?",
									 true, false));
		
		templates.add(
				new QuestionTemplate("CONJ", "and", "what", 1, "... ### what ?",
									 false, true));
	}
}
