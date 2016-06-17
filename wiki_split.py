#!/usr/bin/env python
# -*- coding: utf-8 -*-
#ROPERS MERWAN

import re, os, sys, urllib2

def scan(outstream):
    response = urllib2.urlopen('https://fr.wikipedia.org/wiki/Liste_de_salles_de_spectacle_en_France')
    list = response.readlines()
    DEBUT = '<h3><span class="mw-headline" id="Alsace-Champagne-Ardenne-Lorraine">Alsace-Champagne-Ardenne-Lorraine</span><span class="mw-editsection"><span class="mw-editsection-bracket">[</span><a href="/w/index.php?title=Liste_de_salles_de_spectacle_en_France&amp;veaction=edit&amp;vesection=3" title="Modifier la section : Alsace-Champagne-Ardenne-Lorraine" class="mw-editsection-visualeditor">modifier</a><span class="mw-editsection-divider"> | </span><a href="/w/index.php?title=Liste_de_salles_de_spectacle_en_France&amp;action=edit&amp;section=3" title="Modifier la section : Alsace-Champagne-Ardenne-Lorraine">modifier le code</a><span class="mw-editsection-bracket">]</span></span></h3>\n'
    FIN = '<li id="cite_note-1"><span class="noprint renvois_vers_le_texte"><a href="#cite_ref-1">\xe2\x86\x91</a></span> <span class="reference-text"><a rel="nofollow" class="external free" href="http://www.sep86.fr/projet-200">http://www.sep86.fr/projet-200</a></span></li>\n'
    preD =  ''.join(list)
    firstcut = find_between(preD, DEBUT,FIN)
    array = []
    while find_between(firstcut,"<tr>", "</tr>") != "vide" :
    	array.append(find_between(firstcut,"<tr>", "</tr>"))
    	firstcut = firstcut[len(find_between(firstcut,"<tr>", "</tr>")):]
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
	exit('Veuillez choisir un fichier de sortie')

for outstream in sys.argv[1:] :
	print scan(outstream)
	
