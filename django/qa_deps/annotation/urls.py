from django.conf.urls import patterns, url

from annotation import views

urlpatterns = patterns('',
	# eg: /annotation/
	url(r'^$', views.index, name='index'),

	# eg: /annotation/0.html
	url(r'^(?P<sid>\d+)/$', views.sentence_annotation, name='sentence_annotation'),

	# eg: 
	url(r'^(?P<sid>\d+)/submit/$', views.sentence_submit, name='sentence_submit'),
	
	# eg: /annotation/dashboard/
	url(r'^dashboard/$', views.dashboard, name='dashboard'),
)
