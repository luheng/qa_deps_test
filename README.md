Finding Dependency Structure using Question-Answer Pairs
============

#Overview

#Models

##Arc-Factored Model

#Technique

##Dual Decomposition

#Annotation

##Aligning Question-Answer Words to the Original Sentence

##Validating Constraints


###Constraints to Validate


   * AnswerIsSingleSpanConstraint
   * EntireAnswerIsSingleSpanConstraint: We might want to use this instead of the first one. Useful for finding typos in annotation.
   * AnswerIsSubtreeConstraint: 10 answers that are not a subtree
   * AnswerIsHeadlessSubtreeConstraint: Out of the 258 qa pairs, only 2 answers violates both “subtree” and “headless subtree” constraints.
   * NonEmptyQuestionConstraint
   * NonOverlappingQAConstraint
   * SingleEdgeQAConstraint: Too many violating QAs (44, even after fixing lots of typos)
   * ReversedEdgeQAConstraint
   * LinkedQAConstraint: More relaxed than SingleEdgeQA and ReversedEdgeQA - we only ask there is some connection between question and answer words.

###Problems


   * The “by” and “of” problem, or “in” (prepositional head)
   * Typos and punctuation problem
   * Auxiliary verb problem - i.e. did … say ? and does … say?
   * Annotation might be wrong. i.e.
2084  The announcement follows several comments by government officials that the government will speed up the repeal of the tax , which was originally scheduled to fall with the start of the single internal market in the European Community at the end of 1992 .
QA6  What was in the European Community ? The single internal market.

   * Alignment ambiguity. i.e 2 words “from” in the sentence.

###Validation Results of Constraints
Total number of QA pairs: 258 

Constraint 1: The entire answer is a continuous span in the original sentence (violated by 1)
Only one answer violated this constraint, because it has the special “…. , according to Mr. Hardiman, ….” structure. So this is an easy one.
I find a lot of typos while checking this constraint. We can improve the annotation UI to avoid this problem in the future.

Constraint 2: The answer has exactly one out-going edge. (violated by 7)
9 answers violated this constraint, which is only 3%. So this is a very useful constraint.

Constraint 3: Relaxing constraint 2 - The answer has either one out-going edge, or multiple out-going edges that lead to the same node. This is intended to fix the cases such as “He said that it’s good. - What did he say? - It’s good”. (violated by 0)
Actually there was one answer that violated this constraint, but I realized that I made a annotation mistake. So I went back to fix it.

Constraint 4: The parent of the answer’s head (found in constraint 3) is included in the question. (violated by 42)
This constraint is violated by 16% of the QA pairs.

Constraint 5: Relaxing constraint 4 - Either the question has an edge leading to the head of the answer, or the answer has an edge leading to the question. (violated by 32)
This constraint is violated by 13% of the QA pairs.

I went through a bunch of the violating cases, and here are the main reasons:
1. Prepositional head. If the sentence contains the following structure:
“… by …” , then we will have [question] -> by -> [head of answer].

2. Alignment ambiguity.
I used greedy alignment - always prefer to match the longest span. If there are multiple candidates with same length, then match to the first encountered one. This works really well for matching answers. But questions are much messier because word order is changed. Sometimes single word spans such as “from” in the question got mismatched. I can change the aligner to prefer words that are close to the matched answer.

3. Some gold dependencies that don’t really make much sense to me.
i.e. In the sentence,
Besides the Machinists pact , accords representing 30,000 of the company 's engineering and technical employees in the Puget Sound and Wichita , Kan. , areas expire in early December .
The “In” of "in the Puget Sound and Wichita , Kan. , areas” depends on “30,000” instead of “employees”.

###Conclusion:
- Constraints on answers are easy to enforce, if we are careful about designing the annotation UI to avoid typos and tokenization problems.
- Aligning question words is difficult because word orders are messed up.
- I didn’t go through all the violating cases for question constraints. Some of them are related to prepositional head (i.e. of, by, from). Others seem to be about semantic relations that are not directly captured in the syntactic structure.
- 


#Other Annotation Methods We Thought About


###Moving Around Phrases (Traditional Constituency Test)


###Getting Syntactic Hint from Textual Entailment Pairs


###Getting Syntactic Hint from Shortened Sentences


Here is an example:

Until then, she’s shooting a project on pollution in the outskirts of her adopted city and dreaming up ways to incorporate her years of designexperience with her newfound career.
Shortened sentences:
- She is shooting a project.
- The project is about pollution in the outskirts of her adopted city.
- There is pollution in the outskirts of the city.
- She is dreaming up ways to incorporate her design experience with her newfound career.
- She want to incorporate her design experience with her newfound career.
- She has years of design experience.


#Parser Evaluation




#References
