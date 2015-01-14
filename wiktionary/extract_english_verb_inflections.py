import sys,re

# label = tps (third-person singular), prespart (present participle), simpast (simple past), pastpart (past participle) 

def insert_inflection(verb_infl, verb, label, infl):
	if not verb in verb_infl:
		verb_infl[verb] = {}
	vmap = verb_infl[verb]
	if not label in vmap:
		vmap[label] = []
	vmap[label].append(infl)


def process_inflections(verb_infl, verb, infl):
	# Handle some special cases here:
	if len(infl) == 1 and infl[0] == "ies":  # i.e. carries
		stem = verb[:-1]
		infl = [stem + "ies", verb + "ing", stem + "ied"]
	elif len(infl) == 2 and infl[-1] == "es":
		if infl[0] + "e" == verb:
			infl = [infl[0], ]
		elif infl[0] == verb:
			infl = ["es"]
		else: # i.e. quiz, quizzes, quizzing, quizzed
			stem = infl[0]
			infl = [stem + "es", stem + "ing", stem + "ed"] 
	elif len(infl) == 2 and (infl[-1] == "ing" or infl[-1] == "ed"):
		infl = [infl[0], ]
	elif (len(infl) == 2 and infl[1] == "ies") or (len(infl) == 3 and infl[1] + infl[2] == "ied"): # i.e. carry, carries, carrying, carried
		stem = infl[0]
		infl = [stem + "ies", verb + "ing", stem + "ied"]
	elif len(infl) == 3 and infl[1] == "y" and infl[2] == "ing": # i.e. tie, ties, tying, tied
		infl = [verb + "s", infl[0] + "ying", verb + "d"]

	# 1. Regular verbs, add -s, -ing, -ed and -ed
	if len(infl) == 0:
		insert_inflection(verb_infl, verb, "tps", verb + "s")
		insert_inflection(verb_infl, verb, "prespart", verb + "ing")
		insert_inflection(verb_infl, verb, "simpast", verb + "ed")
		insert_inflection(verb_infl, verb, "pastpart", verb + "ed")
	elif len(infl) == 1:
		if infl[0] == "es":
			insert_inflection(verb_infl, verb, "tps", verb + "es")
		else:
			insert_inflection(verb_infl, verb, "tps", verb + "s")

		if infl[0] + "e" == verb or infl[0][:-1] == verb:
			insert_inflection(verb_infl, verb, "prespart", infl[0] + "ing")
			insert_inflection(verb_infl, verb, "simpast", infl[0] + "ed")
			insert_inflection(verb_infl, verb, "pastpart", infl[0] + "ed")
		else:
			insert_inflection(verb_infl, verb, "prespart", verb + "ing")
			if infl[0] == "d":
				insert_inflection(verb_infl, verb, "simpast", verb + "d")
				insert_inflection(verb_infl, verb, "pastpart", verb + "d")
			else:		
				insert_inflection(verb_infl, verb, "simpast", verb + "ed")
				insert_inflection(verb_infl, verb, "pastpart", verb + "ed")

	elif len(infl) == 2:		
		print "unidentified: ", verb, infl

	# Irregular verbs	
	elif len(infl) == 3:
		insert_inflection(verb_infl, verb, "tps", infl[0])
		insert_inflection(verb_infl, verb, "prespart", infl[1])
		insert_inflection(verb_infl, verb, "simpast", infl[2])
		insert_inflection(verb_infl, verb, "pastpart", infl[2])

	else:
		insert_inflection(verb_infl, verb, "tps", infl[0])
		insert_inflection(verb_infl, verb, "prespart", infl[1])
		insert_inflection(verb_infl, verb, "simpast", infl[2])
		insert_inflection(verb_infl, verb, "pastpart", infl[3])	


def process(filename):
	f = open(filename, 'r')
	word = ""
	infl = ""

	verb_map = {}

	for line in f:
		line = line.strip()
		if "<title>" in line:
			wlen = line.find("</title>") 
			word = line[7 : wlen]
		elif "{{en-verb" in line and not "<comment>" in line:
			infl = line[2:line.find("}}")].split("|")
			# ignore multile word phrases for now
			if " " in word or ":" in word or "-" in word or "en-verb form" in infl[0]:
				continue
			
			infl = [s for s in infl if not s == "en-verb" and not "=" in s and not ":" in s]

			#if len(infl) == 1 and infl[0] + "e" != word and infl[0] != "es" and infl[0] != "d" and infl[0] != word + word[-1] and infl[0] != word:
			#if len(infl) == 2 and infl[1] != "es" and infl[1] != "d" and infl[1] != "ing" and infl[1] != "ed" and infl[1] != "ies":
			#if len(infl) >= 2 and infl[-1] == "ing" and infl[0] + "e" != word and infl[0][:-1] != word:
			#if len(infl) >= 2 and infl[-1] == "ed":# and infl[0] + "e" != word:
			#if len(infl) > 4:
			#	print word, infl

			if word in verb_map:
				infl0 = verb_map[word]
				if len(infl) == 0:
					continue
				elif len(infl0) == 0:
					verb_map[word] = infl
				#elif " ".join(infl0) != " ".join(infl):
				#	continue
				#	print "duplicate entries: ", word, verb_infl[word], infl
			verb_map[word] = infl

	verb_infl = {}
	for verb in verb_map.keys():
		process_inflections(verb_infl, verb, verb_map[verb])	

	
	verbs = sorted(verb_infl.keys())
	for verb in verbs:
		infl = verb_infl[verb]
		print "%s\t%s\t%s\t%s\t%s" % (verb, "/".join(infl["tps"]), "/".join(infl["prespart"]), "/".join(infl["simpast"]), "/".join(infl["pastpart"]))

	f.close()

if __name__ == "__main__":
	process(sys.argv[1])
