#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
#
# Script permettant de parcourir la base de données (https://claude.wtf/artists?numberToReturn=1&offset=1)
# et d'en extraire le nom de l'artiste ainsi que son site web renseigné.
#
# Mode d'emploi : 
# Argument : Numéro de l'artiste sur la base de données (facultatif, 0 par défaut)
# 	$ ./read_bdd.py 
# 	$ ./read_bdd.py 42
# Valeur retournée : 
#	Tableau contenant le nom de l'artiste [0] et son site web [1].
#	La valeur 'None' est retournée si l'artiste n'as pas renseigné son site web.
# 		(u'Nevercold', u'nevercoldmetal.com')
# 		(u'POINT G', 'None')
#	En cas d'erreur du chargement de la page ou autre : GET_ERROR [0] et [1].
#		('GET_ERROR', 'GET_ERROR')
#
#	Le script effectuera un print uniquement si celui-ci est lancé directement. (pour les phases de test)



import urllib, json ,sys
reload(sys)
sys.setdefaultencoding('utf-8')
def bdd_extract(artiste_num) : 
	#file = open('read_bdd_out.txt', 'a')
	for x in range(artiste_num, artiste_num + 1) : #Boucle dans le cas où l'on voudrais extraire toute la base de données dans un fichier texte.
		url = "https://claude.wtf/artists?numberToReturn=1&offset="+str(x)
		f = urllib.urlopen(url)
		try : web_page = json.loads(f.read())
		except ValueError : return "GET_ERROR","GET_ERROR"
		if len(web_page) == 0 : return "GET_ERROR","GET_ERROR"
		if len(web_page[0]['artist']['websites']) != 0  : 
			return(web_page[0]['artist']['name'], web_page[0]['artist']['websites'][0])
			#file.write(web_page[0]['artist']['name']+"\t"+web_page[0]['artist']['websites'][0] + "\n")	
		else : 
			web_page[0]['artist']['websites'].append("None")
			return web_page[0]['artist']['name'], web_page[0]['artist']['websites'][0]
			#file.write(web_page[0]['artist']['name']+"\tNone\n")
	#file.close()


if sys.argv[0] == "./read_bdd.py" : 
	if len(sys.argv) == 1 : print bdd_extract(0)
	else : print bdd_extract(int(sys.argv[1]))