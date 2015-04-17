#! /usr/bin/env python

# http://usgenweb.org/research/nicknames.shtml
import json
name2nicks = []
f = open('nick.txt')
for line in f:
    left, right = line.split("--")
    for w in left.split(" || "):
        o = {"word": w, "pos": "NOUN", "SYNONYM": right.strip()}
        name2nicks.append(o)
f.close()

print json.dumps(name2nicks, indent=4, separators=(',', ': '))
