#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
from twitter_data_extract import twitter_search
from read_bdd import bdd_extract
import os,sys

follower_number = -1 ; found = 0 ; start = 1
f = open('main sortie.txt', 'w')
try : 
	for x in range (start , 600) : 
		print "#", x
		bdd_info =  bdd_extract(x)
		print bdd_info[1],"\n"
		if bdd_info[0] != "GET_ERROR" :
			twitter_info = twitter_search(bdd_info[0])
			if bdd_info[1] != "None" : 
				for y in range(0, len(twitter_info[1])) : 
					if twitter_info[1][y] != "None" and twitter_info[1][y] is not None :
						print twitter_info[1][y], "  [",twitter_info[2][y],"]"
						if bdd_info[1] in twitter_info[1][y] :
							print "MATCH"
							found +=1
							if twitter_info[2][y] > follower_number : follower_number = twitter_info[2][y]
						#if bdd_extract(y)[1] in str(twitter_info[1][y]) : print "Ressemblance trouvé"
				if follower_number != -1 : 
					f.write("#"+str(x)+"\t"+bdd_info[0]+"\t"+str(follower_number)+"\n")
					print "\nnombres de followers :", follower_number
					
except KeyboardInterrupt: 
	if (x-start) != 0 : 
		from decimal import *
		rate = (Decimal(found) / (Decimal(x)-Decimal(start)) ) * 100
	else : rate = 0
	print "\n",found,"compte Twitter trouvé sur", x-start,"\nMatch rate :", float(rate),"%"
	
