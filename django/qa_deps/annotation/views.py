from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render
from django.template import RequestContext, loader

from annotation.models import Sentence, QuestionAnswer

def index(request):
	template = loader.get_template('annotation/index.html')
	sentences = Sentence.objects.all().order_by('sentence_id')[:10]
	context = RequestContext(request, { 'sentences' : sentences })
	return HttpResponse(template.render(context))

def sentence_annotation(request, sid):
	try:
		sentence = Sentence.objects.get(sentence_id=sid)
	except Sentence.DoesNotExist:
		raise Http404
	return render(request, 'annotation/sentence_annotation.html', {'sentence': sentence})

def sentence_submit(request, sid):
	try:
		sentence = Sentence.objects.get(sentence_id=sid)
		annotated_question = request.POST['Q0']
		annotated_answer = request.POST['A0']
	except Sentence.DoesNotExist, KeyError:
		raise Http404
	else:
		qa = QuestionAnswer.create(sentence=sentence, question=annotated_question, answer=annotated_answer)
		qa.save()
        #return HttpResponseRedirect('annotation:sentence_annotation', args=(sid+1,))	
        return HttpResponseRedirect('../../')	

def dashboard(request):
	return HttpResponse("Show annotation results!")
