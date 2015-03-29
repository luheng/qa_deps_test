import sys,re


def process(filename, outfile):
	f = open(filename, 'r')
	word = ""
	vp_pattern = re.compile(r"\[\[\w+]]")

	verb_phrase_list = []

	for line in f:
		line = line.strip()
		if "<title>" in line:
			wlen = line.find("</title>") 
			word = line[7 : wlen]
		elif "{{en-verb" in line and not "<comment>" in line:
			info = line[2:line.find("}}")].split("|")
			if len(info) < 2 or not "head="  in info[1]:
				continue
				
			verb_phrase = [s[2:-2] for s in re.findall(vp_pattern, info[1])]
			if len(verb_phrase) < 2:
				continue
			
			vp = " ".join(verb_phrase)
			verb_phrase_list.append(vp)
			num_vp = len(verb_phrase_list)
			if num_vp % 500 == 0:
				print "Number of extracted verb phrases:", num_vp

	f.close()

	f = open(outfile, 'w')
	for vp in sorted(verb_phrase_list):
		f.write(vp + "\n")
	f.close()
		

if __name__ == "__main__":
	process(sys.argv[1], sys.argv[2])
