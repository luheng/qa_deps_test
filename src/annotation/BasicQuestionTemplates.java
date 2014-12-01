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
		templates.add(
				new QuestionTemplate("VERB", "", "who", 0, "Who ### that ?"));
		templates.add(
				new QuestionTemplate("VERB", "", "what", 1, "What did X ### ?"));
		templates.add(
				new QuestionTemplate("VERB", "", "how", 0, "How did X ### that ?"));
		templates.add(
				new QuestionTemplate("VERB", "", "when", 0, "When did X ### that ?"));
		templates.add(
				new QuestionTemplate("VERB", "", "where", 0, "Where did X ### that ?"));
		templates.add(
				new QuestionTemplate("NOUN", "", "what", 0, "What kind of ### ?"));
		templates.add(
				new QuestionTemplate("ADJ", "", "how", 0, "How ### ?"));
	}
}
