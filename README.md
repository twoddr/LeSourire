# Le Sourire — Gestion du cabinet dentaire

Logiciel de gestion du Cabinet Dentaire **Le Sourire** (Dr Nadine Towe, Douala) :
patients, rendez-vous et rappels automatiques, facturation en lettres-clés D/Z,
stock, comptabilité et comptes utilisateurs par rôle.

## Architecture

```
┌────────────────────┐   HTTP / REST (JSON)   ┌──────────────────────────────┐
│  Client JavaFX     │ ─────────────────────► │  Serveur Spring Boot         │
│  (postes cabinet,  │                        │  (PC principal du cabinet)   │
│   Windows)         │ ◄───────────────────── │  · API REST + sécurité       │
└────────────────────┘                        │  · Rappels J-1 / revisites   │
                                              │  · Sauvegardes automatiques  │
        ... x N postes                        │  · Migrations BD (Flyway)    │
                                              └──────────────┬───────────────┘
                                                             │ JDBC
                                                     ┌───────▼───────┐
                                                     │    MariaDB    │
                                                     └───────────────┘
```

| Module    | Rôle                                                                  |
|-----------|-----------------------------------------------------------------------|
| `commun`  | DTO et énumérations partagés entre le serveur et le client            |
| `serveur` | API REST, base de données, tâches planifiées (rappels, sauvegardes)   |
| `client`  | Application de bureau JavaFX (thème AtlantaFX)                        |

Le serveur est le seul composant qui parle à la base de données. Il tourne en
permanence, ce qui permet d'envoyer les rappels (J-1 avant rendez-vous,
revisites post-intervention) même si aucun poste client n'est allumé.

## Version

Une seule valeur à modifier pour une release : la propriété `<revision>` dans le
`pom.xml` racine (actuellement `0.1.0`). Les modules `commun` / `serveur` /
`client` référencent `${revision}` ; le titre de la fenêtre client affiche cette
version, et le serveur l'expose via `META-INF/build-info.properties` sur
`/api/systeme/statut` (aucune version « en dur » ailleurs).

- JDK 21
- Maven 3.9+
- MariaDB (locale, ou via `docker compose up -d`)

## Démarrage rapide

```bash
# 1. Base de données : au choix
docker compose up -d                    # option A : conteneur
sudo mariadb < scripts/creer_bd_dev.sql # option B : MariaDB déjà installée

# 2. Compiler
mvn package -DskipTests

# 3. Lancer le serveur (applique les migrations Flyway au démarrage)
java -jar serveur/target/lesourire-serveur-0.1.0.jar

# 4. Lancer le client
mvn -pl client javafx:run
```

Compte initial : `admin` / `admin` (**à changer** dès que le module
Administration sera actif). L'écran de connexion propose aussi un **mode
démonstration** qui présente l'interface sans serveur.

Configuration du serveur par variables d'environnement :
`LESOURIRE_BD_URL`, `LESOURIRE_BD_UTILISATEUR`, `LESOURIRE_BD_MOT_DE_PASSE`,
`LESOURIRE_PORT` (défaut : `8420`), `LESOURIRE_HOME` (racine du paquet ; les
lanceurs la transmettent aussi en `-Dlesourire.home` et elle sert à retrouver
`fichiers/cle-chiffrement.key` quel que soit le répertoire de travail),
`LESOURIRE_CHIFFREMENT_CLE` (clé de chiffrement en base64),
`LESOURIRE_CHIFFREMENT_FICHIER` (chemin imposé de la clé — **vide par défaut** :
le serveur cherche alors `fichiers/cle-chiffrement.key` autour du paquet) et
`LESOURIRE_MYSQLDUMP` (chemin de `mysqldump` si MariaDB n'est pas dans le PATH).
Pour l'envoi réel des SMS, voir
`LESOURIRE_ORANGE_*` dans « Notifications patients » ci-dessous.

## Base de données

Le schéma complet (21 tables, toutes les relations posées dès le départ) vit
dans `serveur/src/main/resources/db/migration/` et est appliqué automatiquement
par Flyway :

- `V1__schema_initial.sql` — utilisateurs/rôles, patients, assureurs/sociétés,
  nomenclature en lettres-clés (D/Z, valeurs **versionnées** dans le temps),
  rendez-vous, rappels, actes, factures/paiements, stock, audit, paramètres ;
- `V2__donnees_initiales.sql` — compte admin, tarifaire officiel du cabinet
  (D = Z = 1 200 FCFA), paramètres (fuseau `Africa/Douala`, devise XAF...) ;
- `V3__suivi_paiements_par_payeur.sql` — montants payés/soldes par payeur sur
  la facture (colonnes générées), maintenus par triggers sur `paiement`, et
  vue `v_facture_relance` pour les relances ;
- `V4__historique_couverture_patient.sql` — couvertures assureur/société
  **historisées** (`patient_couverture` : jamais de modification, une clôture
  avec motif puis une nouvelle ligne), trigger anti-chevauchement ;
- `V5__triggers_stock_et_categories.sql` — mise à jour automatique de
  `article.quantite_stock` par triggers sur `mouvement_stock`, catégories
  d'articles initiales ;
- `V6__recreer_triggers_stock.sql` et `V7__recreer_definer_paiement_couverture.sql`
  — recréation des procédures, triggers et vues sans `DEFINER` figé (corrige
  l'erreur « definer does not exist » selon l'utilisateur MySQL du serveur) ;
- `V8__chiffrement_donnees_sensibles.sql` — élargissement des colonnes
  contenant des données sensibles (identité et dossier médical des patients,
  coordonnées des utilisateurs, n° d'assuré) et suppression des index devenus
  inutiles (le tri/recherche se fait en mémoire, cf. ci-dessous) ;
- `V9__mot_de_passe_admin_bcrypt.sql` — le compte initial n'est plus stocké avec
  `{noop}` mais sous forme d'empreinte `{bcrypt}` ;
- `V10__notifications_patients.sql` — notifications patients : canal préféré et
  consentement sur la fiche patient, nouveau type de rappel `CONFIRMATION_RDV`
  (envoyé dès la prise de rendez-vous) et paramètres `notification.*` (voir
  « Notifications patients » ci-dessous) ;
- `V11__rappel_un_jour_avant.sql` — le rappel de rendez-vous part la veille
  (J‑1) au lieu de J‑2 (`rappel.jours_avant_rdv`, modifiable dans Paramètres).

### Chiffrement des données sensibles

L'application chiffre **au repos** (AES‑256‑GCM) les données personnelles et
médicales : identité et coordonnées des patients, antécédents/allergies/notes,
motifs et notes de rendez‑vous, observations d'actes, identité et coordonnées
des utilisateurs, numéro d'assuré, contenu des rappels et journal d'audit
(28 colonnes ; inventaire de référence : `ColonnesChiffrees`). Le format stocké
est `enc:v1:<base64>` ; une valeur historique non préfixée (en clair) reste
lisible et est chiffrée à sa prochaine modification.

La clé (`fichiers/cle-chiffrement.key`) est **versionnée dans le dépôt** et
livrée dans chaque paquet : sans cela, un serveur packagé en générerait une autre
et rendrait illisibles les données déjà chiffrées — c'est exactement l'incident
corrigé ici. Pour un autre cabinet, on remplace ce fichier avant d'assembler les
paquets. La clé est résolue dans l'ordre suivant :

1. `lesourire.chiffrement.cle` (`LESOURIRE_CHIFFREMENT_CLE`, base64) ;
2. le fichier indiqué par `lesourire.chiffrement.fichier`, s'il est renseigné
   (chemin relatif résolu depuis la racine du paquet) ;
3. `fichiers/cle-chiffrement.key`, cherché de façon **déterministe** autour de
   l'installation : racine transmise par les lanceurs (`-Dlesourire.home`),
   dossier du JAR et ses parents, répertoire courant et ses parents ;
   l'emplacement hérité `serveur/fichiers/cle-chiffrement.key` des paquets
   Windows 0.1.x reste accepté ;
4. à défaut, une clé est générée dans `<installation>/fichiers/` : ce cas est
   journalisé en **ERREUR**, car il rend illisibles les données existantes.

**Sauvegardez ce fichier avec la base de données** : sans lui, les données
chiffrées sont définitivement illisibles (une sauvegarde SQL seule ne suffit
pas). Le module Sauvegardes copie automatiquement la clé à côté du dump, et le
dossier `fichiers/` contient un `LISEZ-MOI.txt` à l'attention du cabinet.

Deux garde‑fous accompagnent ce mécanisme :

- `VerificateurCleChiffrement` relit au démarrage un échantillon de chaque
  colonne chiffrée et **refuse de lancer le serveur** si la clé active ne peut
  pas les déchiffrer, en indiquant la colonne fautive, l'empreinte de la clé et
  la marche à suivre ;
- l'outil de secours `Diagnostic-Chiffrement.bat` / `.sh`, embarqué dans les
  paquets, inventorie les clés présentes sur la machine (fichiers, variables
  d'environnement et **archives ZIP/JAR**, donc les paquets successifs), établit
  la compatibilité colonne par colonne, exporte la clé qui relit les données et
  peut réchiffrer la base vers une clé unique
  (`--rechiffrer <clé|fichier> --je-confirme` : sauvegarde obligatoire, refus si
  le serveur tourne, lignes illisibles laissées intactes). Rapport :
  `DIAGNOSTIC-CHIFFREMENT.txt`, sans aucune donnée patient.

Comme le nom, le prénom et les téléphones sont chiffrés, la recherche patient /
utilisateur / facture ne se fait plus en SQL mais **en mémoire**, sur les
valeurs déchiffrées (volumes d'un cabinet : largement suffisant).

### Sauvegardes

Le module Sauvegardes du client — et le script `Sauvegarder-Base.bat` / `.sh`
livré dans les paquets — écrit un dump `mysqldump` **et copie la clé de
chiffrement à côté** : une sauvegarde de la base seule ne permet pas de relire
les données. Les deux fichiers vont toujours ensemble.

- **dossier d'écriture** : paramètre `sauvegarde.dossier` (table `parametre`,
  Administration → Paramètres) ; relatif, il est résolu depuis la **racine du
  paquet** (et non plus depuis le répertoire de travail), et le dossier retenu
  est journalisé au démarrage : `Sauvegardes : dossier …` ;
- **programme `mysqldump`** : résolu par `lesourire.sauvegarde.mysqldump`
  (`LESOURIRE_MYSQLDUMP`), sinon par le `PATH`, sinon dans les dossiers
  d'installation MariaDB/MySQL (`C:\Program Files\MariaDB *\bin\mysqldump.exe`,
  `/usr/bin`, `/usr/local/bin`…) — inutile de modifier le `PATH` de Windows ;
- toute erreur est remontée avec son message exact (chemin cherché, sortie de
  `mysqldump`) au lieu d'un « 500 » opaque.

Scripts livrés dans chaque paquet : `ToutDemarrer` (serveur + client),
`Lancer-Serveur-Debug` (serveur en fenêtre/terminal visible, erreur affichée),
`Sauvegarder-Base` (dump + clé), `Diagnostic-Chiffrement` (inventaire des clés),
`Arreter-Serveur`. Les lanceurs affichent la **version du paquet** et
l'**empreinte de la clé**, acceptent un chemin contenant des espaces, et
l'assembleur vérifie automatiquement les lanceurs `.bat` (apostrophes appariées,
aucun chemin variable dans un `-ArgumentList`) pour qu'une faute de ce genre ne
puisse plus être livrée.

### Notifications patients (SMS, WhatsApp, e-mail)

Au Cameroun le SMS est le canal réellement lu : c'est donc lui qui part
**automatiquement**. Le fournisseur retenu est l'API **SMS Cameroon 2.0**
d'Orange (`developer.orange.com`), qui couvre tous les opérateurs (MTN et
Orange) et dont les crédits s'achètent par paquets, payés par Orange Money ou
avec le crédit d'une carte SIM Orange.

Le parcours est le suivant :

1. à l'enregistrement d'un rendez-vous, une **confirmation** part vers le
   patient (moins d'une minute après, le temps d'une passe du planificateur) ;
2. **la veille** (paramètre `rappel.jours_avant_rdv`, 1 jour par défaut), un
   rappel est envoyé ; un rendez-vous annulé ou déplacé annule automatiquement
   les envois encore en attente ;
3. rien n'est programmé si la case « le patient accepte d'être prévenu » est
   décochée sur sa fiche.

Le canal se règle **par patient** (champ « Rappels par ») : `Automatique`
(SMS si un numéro est renseigné, sinon WhatsApp, sinon e-mail), `SMS`,
`WhatsApp` ou `E-mail`.

WhatsApp et l'e-mail sont des canaux **assistés** : le serveur prépare le
message et un lien `wa.me` / `mailto:`, le secrétariat clique puis confirme
l'envoi. Ils ne demandent donc aucun compte Meta ni serveur SMTP. Le panneau
**« Notifications à envoyer »** (colonne de droite de l'Agenda) liste les
envois restants, permet de forcer un envoi, de marquer un envoi assisté comme
fait ou de l'annuler. Chaque ligne rappelle la date d'envoi prévue et le
rendez-vous concerné ; le bandeau du panneau compte les envois **en attente** et
**envoyés**, et le bouton **Historique** ouvre le journal des derniers envois
(date, patient, motif, canal, destinataire, rendez-vous ; le message complet
apparaît en infobulle).

Une notification peut aussi être créée à la main : un **clic droit sur un
rendez-vous** de l'agenda ouvre un menu qui permet de modifier la fiche, de
changer le statut (planifié, confirmé, en salle d'attente, honoré, absent,
annulé) ou de **notifier le patient**. La notification est alors rattachée au
rendez-vous cliqué — le dialogue en rappelle le patient, la date, le praticien
et le téléphone — avec choix du motif (confirmation, rappel, revisite), du canal
et du message ; un message laissé vide reprend le modèle standard du cabinet.
Le patient doit toujours accepter d'être prévenu, et le canal demandé doit
exister sur sa fiche (sinon le serveur explique le refus au lieu de changer de
canal en silence).

Repères souris de l'agenda : **double-clic** sur un bloc ouvre la fiche,
**double-clic dans une zone vide** crée un rendez-vous sur le créneau visé,
**simple clic dans le vide** désélectionne.

Réglages (Administration ▸ Paramètres) : `notification.active` (interrupteur
général des envois automatiques, `false` par défaut), `notification.fournisseur`
(`orange` pour l'envoi réel, `journal` pour simuler en écrivant dans le journal
du serveur), `notification.indicatif_pays`, `notification.nom_expediteur` et
`notification.max_tentatives`. Un envoi qui échoue est retenté 15 minutes plus
tard, jusqu'à ce nombre de tentatives, puis passe en `ECHEC` avec le motif
affiché.

Les identifiants Orange (`LESOURIRE_ORANGE_CLIENT_ID`,
`LESOURIRE_ORANGE_CLIENT_SECRET`, `LESOURIRE_ORANGE_EXPEDITEUR`) passent
uniquement par l'environnement du serveur (voir
`packaging/lesourire/serveur/lesourire-serveur.conf.sh|.bat`) : la table
`parametre` n'étant pas chiffrée, un secret n'a rien à y faire.

Points de vigilance : l'API Orange accepte **5 SMS par seconde** (le
planificateur temporise donc entre deux envois) ; un SMS au-delà de 160
caractères est facturé en plusieurs parties (les modèles livrés tiennent en un
seul) ; l'API exige un accès Internet depuis le serveur du cabinet.

### Points de sécurité à connaître (bêta)

- L'API utilise **HTTP Basic sans TLS** : les identifiants et les réponses
  circulent en clair sur le réseau du cabinet. À réserver au réseau local de
  confiance ; un reverse‑proxy HTTPS est recommandé dès que le serveur sort de
  ce réseau.
- Le compte initial `admin` / `admin` doit être changé à la première connexion
  (le mot de passe n'est plus stocké en clair, mais reste trivial).

Toute évolution du schéma = un nouveau fichier `V<n>__description.sql`,
appliqué automatiquement chez le client à la mise à jour du serveur.

Pour créer la base **sans passer par le serveur** (import direct MariaDB) :
`scripts/lesourire_complet.sql` contient tout le schéma V1→V10, les données
initiales et l'historique Flyway (le serveur démarre dessus sans rien rejouer).

## Factures PDF

L'aperçu / impression d'une facture est un PDF **A4 généré depuis un template
HTML** (`client/src/main/resources/templates/facture.html`) rendu par
OpenHTMLtoPDF, avec la police DejaVu embarquée (`client/src/main/resources/fonts/`)
et le logo du cabinet (`client/src/main/resources/images/logo.png`) : la mise en
page se peaufine donc dans le CSS du template, sans toucher au code Java
(`FacturePdf`, qui ne fait que l'alimenter et l'écrire).

L'identité du cabinet figure sur **chaque page** :

- en tête de la 1re page, à droite du logo : `CABINET DENTAIRE LE SOURIRE`,
  `Docteur Nadine TOWE`, `Chirurgien Dentiste`, diplôme (UFR d'Odonto –
  Stomatologie d'Abidjan, Côte d'Ivoire) et téléphones ;
- en pied de toutes les pages (élément répété par `@page { @bottom-center }`),
  suivi du numéro de page `Page n / N` ;
- les pages 2 et suivantes rappellent seulement le logo + `Facture n° — patient`.

Ces coordonnées sont définies une seule fois, dans les constantes `CABINET_*` de
`FacturePdf` (le template se contente de les mettre en forme). Elles recoupent
les paramètres `cabinet.*` de la base, qui pourront les alimenter plus tard.

## Feuille de route

| Phase | Contenu                                                            | État |
|-------|--------------------------------------------------------------------|------|
| 1     | Squelette : BD complète, serveur, client, connexion, navigation    | ✔    |
| 2     | Module Patients (fiche, recherche, tiers payants, audit)           | ✔    |
| 3     | Agenda / RDV + programmation des rappels J-1 (envoi mail/WA ensuite) | ✔    |
| 4     | Facturation (actes D/Z, remises, quotes-parts, paiements)           | ✔    |
| 5     | Stock (articles, fournisseurs, alertes)                             | ✔    |
| 6a    | Administration (utilisateurs, tarifaire, paramètres, sauvegardes)   | ✔    |
| 6b    | Envoi des rappels (SMS Orange automatique, WhatsApp/e-mail assistés) | ✔    |
| 6c    | Tableau de bord enrichi + comptabilité                              |      |
| 7     | Installeur Windows (jpackage/MSI), service Windows, mises à jour    |      |

## Déploiement (bêta, style Labos)

Deux paquets « zip + double-clic / scripts », sans MSI pour l’instant.

```bash
# Depuis la machine de build (Linux OK) :
./scripts/assembler-executables.sh
```

Si un bureau est disponible, le script signale le début et la fin (succès ou
échec) par une notification : `kdialog` sous **KDE Plasma** (natif), sinon
`zenity` ou `notify-send` — y compris en session **Wayland**.
En complément — et notamment en SSH / sans affichage graphique — il affiche
dans le terminal un indicateur animé (`| / - \` + chronomètre) pendant la
compilation, puis un message de fin clair (`✔ Terminé` ou `✖ Terminé … échec`).
Cet indicateur n'apparaît que sur un vrai terminal : en redirection ou en CI,
la sortie standard et l'erreur restent propres.

**Lancement par double-clic (KDE Plasma / Dolphin)** : un double-clic n'associe
aucun terminal ; le script **ouvre alors automatiquement une console** (Konsole,
sinon `xterm`) où l'indicateur et les journaux restent visibles, et attend une
touche avant de fermer. La relance ne peut pas boucler (marqueur
`LESOURIRE_DANS_TERMINAL`). Les lanceurs Unix du paquet client
(`ToutDemarrer.sh`) appliquent le même mécanisme.

Résultat :
- `out/executables/lesourire-windows/` — `.bat`, JavaFX Windows, emplacement `jre-windows/`
- `out/executables/lesourire-mac-linux/` — `.sh`, JavaFX Linux/macOS, `jre-linux/` + `jre-mac/`

Sur le PC du cabinet :
1. Copier une JRE 21 dans le dossier `jre-*` correspondant (voir `JRE-A-COPIER.txt`)
2. Première install : exécuter `sql/01_creer_bd.sql`
3. Vérifier `serveur/lesourire-serveur.conf.*` (BD, port 8420)
4. `1-Demarrer-Serveur` puis `2-Demarrer-LeSourire` — ou `ToutDemarrer`

`ToutDemarrer` lance le serveur en arrière-plan (fenêtre masquée), affiche un
indicateur animé pendant l'attente, et **n'ouvre le client qu'une fois le
serveur prêt** (réponse sur `/api/systeme/statut`, migrations Flyway comprises).
La détection utilise `curl` (ou `nc` à défaut) ; si aucun des deux n'est
présent, le client n'est pas lancé et un message explique la marche à suivre.
Les logs du serveur sont dans `serveur/logs/lesourire-serveur.log` ; pour
l'arrêter, utiliser `Arreter-Serveur` (`.bat` sous Windows, `.sh` sous Linux/macOS).

Connexion initiale : `admin` / `admin`. Détails dans le `README.txt` de chaque paquet.

Plus tard (phase 7) : MSI via `jpackage`, service Windows, mises à jour auto.
