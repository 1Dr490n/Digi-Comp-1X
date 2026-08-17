import re

# set all registers in a to the according registers in b if condition is met.
# @set(ABC, DEF, X)
# expands to
# set A XD
# set B XE
# set C XF
def set(a, b, condition):
	if len(a) != len(b):
		raise Exception('a and b must have same amount of registers')
	if re.search('^[a-zA-Z0-9]+$', a) == None:
		raise Exception('a must be list of registers', a)
	if re.search('^[a-zA-Z0-9]+$', b) == None:
    		raise Exception('b must be list of registers', b)

	lines = ['set {} {}{}'.format(a[i], condition, b[i]) for i in range(len(a))]
	return lines

# pres all registers in a if condition is met.
# @pres(ABC, X)
# expands to
# pres A X
# pres B X
# pres C X
def pres(a, condition):
	if re.search('^[a-zA-Z0-9]+$', a) == None:
		raise Exception('a must be list of registers', a)

	lines = ['pres {} {}'.format(x, condition) for x in a]
	return lines

# full bit add registers a and b with carry if condition is met.
def add(a, b, carry, condition):
	if re.search('^[a-zA-Z0-9]$', a) == None:
		raise Exception('a must be one registers', a)
	if re.search('^[a-zA-Z0-9]$', b) == None:
		raise Exception('b must be one registers', b)
	if re.search('^[a-zA-Z0-9]$', carry) == None:
		raise Exception('carry must be one registers', carry)
	return """
set * # !*!+/
set * # !*+!/
set * # *!+!/
set * # *+/

set / # *+
set / # */
set / # +/

pres * #
pres /
	""".replace('*', a).replace('+', b).replace('/', carry).replace('#', condition).splitlines()
