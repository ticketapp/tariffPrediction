#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
#
#
# Script permettant de récupérer la capacité des salles de Spectacle en France
# depuis la page Wikipedia : https://fr.wikipedia.org/wiki/Liste_de_salles_de_spectacle_en_France
# Mode d'emploi : 
# 	$ ./wiki_split.py
#	* ./wiki_split.py Nom_du_fichier_de_sortie.txt (<= facultatif)
# 
# Contenu du fichier de sortie :
#	Ligne 1 : Nom de la ville
#	Ligne 2 : Nom de la salle
#	Ligne 3 : Capacité + "places"
#	Ligne 4 : [Capacité] 

import re, os, sys, urllib2

def scan(outstream):
    response = urllib2.urlopen('https://fr.wikipedia.org/wiki/Liste_de_salles_de_spectacle_en_France')
    list = response.readlines()
    string_1 = '</span><a href="/w/index.php?title=Liste_de_salles_de_spectacle_en_France&amp;action=edit&amp;section=3" title="Modifier la section : Auvergne-Rhône-Alpes">modifier le code</a><span class="mw-editsection-bracket">]</span></span></h3>'
    string_2 = 'capacité de 4000 à 10000 places a été abandonné'
    split_str =  ''.join(list)
    first_cut = find_between(split_str, string_1,string_2)
    print first_cut
    array = []
    while find_between(first_cut,"<tr>", "</tr>") != "vide" :
    	array.append(find_between(first_cut,"<tr>", "</tr>"))
    	first_cut = first_cut[len(find_between(first_cut,"<tr>", "</tr>")):]
    outf = open(outstream, "w")
    for y in range(len(array)) :
    	array[y] = array[y][17:(len(array[y]))-1].split("\n") #17 = len(("<th scope="row">")+1)
    	for z in range(len(array[y])) : 
    		temp = re.sub('<[^>]+>', '', array[y][z])
    		if temp == "Nom de la salle" or temp == "Capacité maximum" or len(temp) == 0 : pass
    		else :
    			if temp.find("#") != -1 : 
    				temp = temp[temp.find("#")+1:]
    				temp = temp.replace(";","")
    				outf.write(temp+"\n")
    				if z == 2 and [int(s) for s in temp.split() if s.isdigit()] : outf.write((str([int(s) for s in temp.split() if s.isdigit()]))+"\n")
    			else : 
    				outf.write(temp+"\n")
    				if z == 2 and [int(s) for s in temp.split() if s.isdigit()] : outf.write((str([int(s) for s in temp.split() if s.isdigit()]))+"\n")
    			if z == 2 : outf.write("\n")
    return "Données spliter et sauvegardé dans le fichier %s" % outstream
    outf.close()

def find_between( s, first, last ):
    try:
        start = s.index( first ) + len( first )
        end = s.index( last, start )
        return s[start:end]
    except ValueError:
        return "vide"

if len(sys.argv) is 1 :
	print scan("wiki_split_out.txt")

for outstream in sys.argv[1:] :
	print scan(outstream)
	
