from django.db import models

class Sentence(models.Model):
	sentence_id = models.IntegerField(default = 0)
	sentence_tokens = models.CharField(max_length = 1024)
	sentence_tags = models.CharField(max_length = 1024)
	sentence_parents = models.CharField(max_length = 1024)

	def __str__ (self):
		return "\t".join([str(self.sentence_id), self.sentence_tokens, self.sentence_tags, self.sentence_parents])

class QuestionAnswer(models.Model):
	sentence = models.ForeignKey(Sentence)
	annotator = models.CharField(max_length = 128)
	question = models.CharField(max_length = 1024)
	answer = models.CharField(max_length = 1024)

	@classmethod
	def create(cls, sentence, question, answer, annotator=""):
		qa = cls(sentence=sentence, question=question, answer=answer, annotator=annotator)
		return qa

	def __str__ (self):
		return "\t".join([str(self.sentence.sentence_id), self.question, self.answer, self.annotator])
