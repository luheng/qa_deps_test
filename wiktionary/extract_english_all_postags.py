import sys,re

# label = tps (third-person singular), prespart (present participle), simpast (simple past), pastpart (past participle) 

def process(filename):
	f = open(filename, 'r')
	word = ""
	infl = ""
	pos_buffer = [] # order is important ...
	
	pos_map = {}
	pos_pattern = re.compile(r"^\{\{en-\w+");

	for line in f:
		line = line.strip()
		if "<title>" in line:
			if len(word) > 0 and len(pos_buffer) > 0:
				if "verb" in pos_buffer:
					print word + "\t" + " ".join(pos_buffer)
				#pos_map[word] = pos_buffer
				pos_buffer = []
			wlen = line.find("</title>") 
			word = line[7 : wlen]
		elif line.startswith("{{en-"):
			matched = re.match(pos_pattern, line)
			if matched is None:
				continue
			pos = matched.group(0)[5:]
			pos_buffer.append(pos)

	f.close()

	#for word in pos_map.keys():
	#	print word, pos_map[word]

if __name__ == "__main__":
	process(sys.argv[1])
