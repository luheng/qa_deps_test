# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('annotation', '0001_initial'),
    ]

    operations = [
        migrations.RenameField(
            model_name='sentence',
            old_name='sentence_text',
            new_name='sentence_tokens',
        ),
        migrations.AddField(
            model_name='sentence',
            name='sentence_parents',
            field=models.CharField(default='', max_length=1024),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='sentence',
            name='sentence_tags',
            field=models.CharField(default='', max_length=1024),
            preserve_default=False,
        ),
    ]
