#!/usr/bin/python
import cgi
import cgitb
import codecs
import json
from os.path import join as join
import socket
import sys

cgitb.enable()

filepath = cgi.FieldStorage()["filename"].value
sys.stderr.write("loading json file: " + filepath + "\n")

output_file = codecs.open(join(filepath), 'w')
output_file.write(json.dumps(sents, indent=4))
output_file.write('\n')
output_file.close()

print 'Content-type: application/json\n\n' 
print json.dumps( {"saved" : "success"} )
