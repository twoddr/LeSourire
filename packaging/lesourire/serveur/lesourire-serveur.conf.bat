@echo off
REM Configuration MariaDB / serveur Le Sourire (a adapter une fois chez le client)

set LESOURIRE_BD_URL=jdbc:mariadb://localhost:3306/lesourire
set LESOURIRE_BD_UTILISATEUR=admin
set LESOURIRE_BD_MOT_DE_PASSE=csa-soft
set LESOURIRE_PORT=8420

REM --- Notifications patients (SMS via l'API Orange Cameroun) ---------------
REM Identifiants de l'application declaree sur https://developer.orange.com
REM (menu "Applications"). Jamais stockes en base : la table `parametre` n'est
REM pas chiffree, ce fichier doit rester lisible par le seul compte du serveur.
REM Laisses vides, le SMS reste en mode "journal" (simule, rien n'est facture).
set LESOURIRE_ORANGE_CLIENT_ID=
set LESOURIRE_ORANGE_CLIENT_SECRET=
REM Adresse d'expedition : nom whiteliste, ou tel:+237XXXXXXXXX pour une SIM.
REM Vide = parametre `notification.nom_expediteur` (Administration > Parametres).
set LESOURIRE_ORANGE_EXPEDITEUR=
