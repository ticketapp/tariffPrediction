#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
#
# Script permettant l'extraction du nombres de followers et de l'URL renseigné sur les comptes 
# Twitter étant susceptible d'être le compte officiel des artistes.
#
# Mode d'emploi : 
# Argument : Nom de l'artiste
# 	$./twitter_data_extract.py Eduardo De La Calle
# Valeur retournée : 
#	3 Tableau contenant respectivement :
#		- La liste des noms des comptes Twitter (name)
#		- La liste des URLS liés à ce compte (expanded_url)
#		- La liste du nombres de followers des comptes (followers_count)
#	Si le compte en question n'as pas renseigné d'URL, la fonction écrira "None" dans la liste.
#	Si aucun utilisateur n'est trouvé les 3 tableau seront vides.

import tweepy,sys
 
# Consumer keys and access tokens, used for OAuth
consumer_key = 'wU7QIC8Sdnv0ne0BX9iUWUuOX'
consumer_secret = 'kf7dKSPF3wLdgWLcbLPrGnawRvfpCN2CJY9TTCqsrt4Gf5ZATf'
access_token = '3229481342-XIhBf8wAGryMxQgQxOp4D3pGtvmtKYBGB7HTESG'
access_token_secret = 't6j9oOZeFZ5WaA3XvFKzvRVizE60xfdEwOusZPElT9okc'

# OAuth process, using the keys and tokens
auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)
api = tweepy.API(auth)

def twitter_search(nom_artiste) : 
	#Eduardo De La Calle
	tw_list = api.search_users(nom_artiste)

	#print "\n\n=======================    NAME   =======================\n\n"
	name = [u.name for u in tw_list]
	#print name, "\n\n=======================    SCREEN_NAME   =======================\n\n"
	screen_name = [u.screen_name for u in tw_list]
	#print screen_name, "\n\n=======================    ID   =======================\n\n"
	id = [u.id for u in tw_list]
	#print id,"\n\n=======================    FOLLOWERS_COUNT   =======================\n\n"
	followers_count = [u.followers_count for u in tw_list]
	#print followers_count,"\n\n=======================   URL   =======================\n\n"
	url = [u.url for u in tw_list]
	#print url,"\n\n=======================    EXPANDED_URL   =======================\n\n"
	entities = [u.entities for u in tw_list]
	expanded_url = []
	for count in entities :
		if 'url' in count : 
			for count1 in count['url']['urls']: expanded_url.append(count1['expanded_url'])
		else : expanded_url.append("None")

# 	print len(name),"utilisateur(s) trouvé.\n"
# 
# 	for x in range(0, len(name)) : 
# 		print "#", x+1
# 		print '\t',name[x]
# 		print '\t',expanded_url[x]
# 		print '\t',followers_count[x],"follower(s) \n"
	
	return name,expanded_url,followers_count


if sys.argv[0] == "./twitter_data_extract.py" : 
	if len(sys.argv) == 1 : print "Veuillez entrez le nom de l'artiste que vous recherchez"
	else : print twitter_search(sys.argv[1:])
#f = open('tw sortie.txt', 'w') ; f.write(str(tw_list)) ; f.close()
