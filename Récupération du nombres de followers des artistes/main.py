#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
#
# Script permettant de faire correspondre les informations extraite depuis la base de données
# et celle obtenu depuis la recherche avec l'API Twitter afin d'exporter les informations
# complémentaires concernant un artiste dans un fichier texte.
#
#
# Fichier indispensable : (à placer dans la même dossier que main.py)
#	- read_bdd.py
#	- twitter_data_extract.py
#
# Mode d'emploi :
#
#	$ ./main.py [-f -s -e -pprint]
#	-f (Nom du fichier de sortie | main_out.txt par défaut.)
#	-s (Entier de départ de la recherche  | 0 par défaut.)
#	-e (Entier de fin de la recherche  | 9999 par défaut.)
#	-pprint (print affiché sur la console  | False par défaut.)
#
#	Si le script ne reçois pas de réponse de la part de la base de données
#	pendant 5 requêtes consécutives le script s'arrêtera.
#
# Exemple :
#
#	$ ./main.py -f sortie.txt -s 100 -e 105 -pprint
#	Le script parcourra la base de données du numéro 100 à 105, écrira sur le fichier sortie.txt
#	et effectueras des print dans l'interprète.
#
#	$ ./main.py
#	Le script parcourra la base de données du numéro 0 à 9999, écrira sur le fichier main_out.txt
#	et n'effectueras pas de print dans l'interprète.
#
# Contenu du fichier de sortie :
#	
#	724677124244103	30
#
#	Pour chaque ligne, les informations sont séparer par une tabulation (\t)
#	1) L'ID Facebook de l'artiste (présent sur la base de données)
#	2) Nombres de followers
#
# Description des print :
#
#  #114 numacrew.com  (Numéro sur la base données, site web de l'artiste renseigné par la base de données)
# 
#		http://www.numacrew.com   [ 1442 ]	(site web du compte twitter portant un nom similaire à celui de l'artiste, nombres de followers du compte)
#		**MATCH**	(Le nom de compte et l'URL étant très similaire à celui sur la base de données, nous concluons que ce compte twitter est bien celui de l'artiste)
#		http://www.facebook.com/tkay.numa   [ 188 ]
#		https://soundcloud.com/arge-numa-crew   [ 82 ]
#
# 	ctrl + c
# 		5 comptes Twitter trouvé sur 14 
#		Match rate : 35.71 %

from twitter_data_extract import twitter_search
from read_bdd import bdd_extract
import os,sys
from decimal import *

def search(file, start, end, pprint) :
	total_found = 0
	f = open(file, 'w')
	try : 
		for x in range (start , end) : 
			follower_number = -1 ; found = 0 ; error_count = 0
			bdd_info =  bdd_extract(x)
			if pprint == True :  print "\n#", x , "\t", bdd_info[1],"\n"
			if bdd_info[0] != "GET_ERROR" :
				error_count = 0
				twitter_info = twitter_search(bdd_info[0])
				if bdd_info[1] != "None" : 
					for y in range(0, len(twitter_info[1])) : 
						if twitter_info[1][y] != "None" and twitter_info[1][y] is not None :
							if pprint == True :  print "\t",twitter_info[1][y], "  [",twitter_info[2][y],"]"
							if bdd_info[1] in twitter_info[1][y] :
								if pprint == True :  print "\t**MATCH**"
								found +=1
								if twitter_info[2][y] > follower_number : follower_number = twitter_info[2][y]
					if follower_number != -1 : 
						f.write(bdd_info[2]+"\t"+str(follower_number)+"\n")
						total_found += 1
			else : error_count += 1
			if error_count == 5 : quit_script(x+1, total_found, f)
		quit_script(x+1,total_found,f)					
	except KeyboardInterrupt: quit_script(x, total_found, f)


def quit_script(x, total_found, f) :
	if (x-start) != 0 : 
		rate = (Decimal(total_found) / (Decimal(x)-Decimal(start)) ) * 100
	else : rate = 0	
	if pprint == True : print "\n",total_found,"comptes Twitter trouvé sur", x-start,"artistes\nMatch rate :", float(rate),"%"
	f.close() ; sys.exit()


if sys.argv[0] == "./main.py" :
	file = "main_out.txt" ; start = 0 ; end = 9999 ; pprint = False 
	for x in range(1, len(sys.argv)) : 
		if sys.argv[x] == '-f' : file = sys.argv[x+1] 
		if sys.argv[x] == '-s' : start = int(sys.argv[x+1])
		if sys.argv[x] == '-e' : end = int(sys.argv[x+1])
		if sys.argv[x] == '-pprint' : pprint = True
	search(file, start, end, pprint)
