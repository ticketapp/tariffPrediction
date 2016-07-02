#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merwan ROPERS
#
#
#
# Script de la GUI finale du projet. Interagis avec l'utilisateur afin de récupérer 
# les données necessaires au calcul du prix du billet.
#
# Mode d'emploi : 
# 	$ ./gui.py
# 
#	1) Entrer l'URL Facebook dans le champs correspondant. (exemple :https://www.facebook.com/events/1698419267079342/)
#		- Ou passez directement à l'étape 3.
#	2) Appuyer sur le bouton Rechercher.
#	3) Compléter les données manquantes si nécessaires.
#	4) Appuyer sur le bouton Envoyé.

from Tkinter import *
import Tkinter
import re, os, requests, urllib, json
import tkMessageBox


#Canvas
canvas = Canvas(width = 550, height = 555, bg = 'white')
canvas.pack(expand = YES, fill = BOTH)
gif1 = PhotoImage(file = 'canvas.gif')
canvas.create_image(0, 0, image = gif1, anchor = NW)    

tete = {'Content-type': 'application/json', 'Accept': 'text/plain'}

form = [
	"attendingCount",
	"declinedCount",
	"interestedCount",
	"maybeCount",
	"noreplyCount",
	"organizersLikes",
	"placeCapacity",
	"placeLike",
	"artistsLikes"
	]
	
form_get = [
	"attending_count",
	"declined_count",
	"interested_count",
	"maybe_count",
	"noreply_count",
	"organizersLikes",
	"placeCapacity",
	"placeLike",
	"artistsLikes"
	]

form_pprint = [
	"Nombres de participants :",
	"Nombres d'invitation refusés :",
	"Nombres de personnes intéréssés :",
	"Nombres de personnes incertaines :",
	"Nombres d'invitation non-répondu :",
	"Nombres de Like de la page de l'organisateur :",
	"Capacité du lieu de l'évènement :",
	"Nombres de Like du lieu de l'évènement :",
	"Nombres de Like de l'artiste :"
	]

#Filtre l'URL puis effectue une requête GET sur la base de données afin de récupérer le fichier Json
def get_ID() :
	for x in range(0, len(form)) :
		apply[x].delete(0, len(apply[x].get()))
	event_found = False 
	fetch_id = re.findall(r'\d+',fb_id.get())
	if len(fetch_id) != 0 : 
		url = "https://claude.wtf/events/" + fetch_id[0]
		response = urllib.urlopen(url)
		try : 
			data = json.loads(response.read())
			event_found = True	
		except : pass
	fb_id.delete(0, len(fb_id.get()))
	if event_found == True :
		fb_id.insert(0, fetch_id[0])
		insert_json_data(data)
		return data
	else : 
		fb_id.insert(0, "Event non trouvé")
		return False

#Insère dans la GUI les données obtenus du fichier Json
def insert_json_data(json_file) :
	for x in range(0, len(form)) :
		try :apply[x].insert(0, json_file['counts'][str(form_get[x])])
		except : apply[x].insert(0, "?")
		
#Compose puis envoi une requête POST au serveur afin de déterminer le prix de revente 
def send_to():
	final_data = {"attendingCount": 1,"declinedCount": 1,"interestedCount": 1,"maybeCount": 1,"noreplyCount": 1,"organizersLikes": [1],"placeCapacity": 1,"placeLike": 1,"artistsLikes": [1]}
	wrong = 0
	for x in range(0, len(form)) :
		can_save = False
		try : 
			int(apply[x].get())
			can_save = True
		except : 
			apply[x].delete(0, len(apply[x].get()))
			apply[x].insert(0, "Entier attendu")
			wrong += 1
		if can_save == True : 
			if not isinstance(final_data[form[x]], int) : final_data[form[x]] = [int(apply[x].get())]
			else : final_data[form[x]] = int(apply[x].get())
	if wrong == 0 : 
		r = requests.post('https://claude.wtf/predictions', data=json.dumps(final_data), headers = tete)
		final_price = r.json()
		display_final_price = "Prix optimal de revente du billet : " + str(final_price) + " €."
		L2 = Label(canvas, text=display_final_price).grid(row=15,column=1,sticky="w", pady=(20,0))
		tkMessageBox.showinfo("Résultat", display_final_price)


#padding
pad_left = Label(canvas).grid(row=5, padx = 5)
pad_center = Label(canvas, text="").grid(row=4,column= 1,sticky="w", ipady = 15)
pad_bottom= Label(canvas, text="").grid(row=20,column= 1,sticky="w", ipady = 2)

#Label
L1 = Label(canvas, text="URL Facebook de l'événement :").grid(row=2,column= 1,sticky="w", pady = (80,0))
L2 = Label(canvas, text="Prix optimal de revente du billet : ").grid(row=15,column=1,sticky="w", pady=(20,0))
for x in range(0, len(form)) :
	text = form[x]+"text"
	text = Label(canvas, text=form_pprint[x]).grid(row=x+5,column= 1, ipady = 5, sticky="w",)

#Entry
fb_id = Entry(canvas, width = 34)
fb_id.grid(row=3, column = 1, sticky="w")
apply = []
for x in range(0, len(form)) :
	apply.append(form[x])
	apply[x]= Entry(canvas, width=9)

#Case
for x in range(0, len(form)) :
	apply[x].grid(row=x+5, column= 2, sticky="w")

#Button
B = Tkinter.Button(canvas, text ="Chercher",  command = get_ID,width=7)
B.grid(row=3, column=2, sticky="w")
send_button = Tkinter.Button(canvas, text ="Envoyer",  command = send_to)
send_button.grid(row=9, column=3, padx = (40,8))

mainloop()