
idx = 0
for line in open('prepositions.txt', 'r'):
	print "\"%s\"," % line.strip(),
	idx += 1
	if idx % 10 == 0:
		print ""

