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
└────────────────────┘                        │  · Rappels J-2 / revisites   │
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
permanence, ce qui permet d'envoyer les rappels (J-2 avant rendez-vous,
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
`LESOURIRE_PORT` (défaut : `8420`), `LESOURIRE_CHIFFREMENT_CLE` (clé de
chiffrement en base64) et `LESOURIRE_CHIFFREMENT_FICHIER` (défaut :
`fichiers/cle-chiffrement.key`). Pour l'envoi réel des SMS, voir
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
  « Notifications patients » ci-dessous).

### Chiffrement des données sensibles

L'application chiffre **au repos** (AES‑256‑GCM) les données personnelles et
médicales : identité et coordonnées des patients, antécédents/allergies/notes,
motifs et notes de rendez‑vous, observations d'actes, identité et coordonnées
des utilisateurs, numéro d'assuré, contenu des rappels et journal d'audit. Le
format stocké est `enc:v1:<base64>` ; une valeur historique non préfixée (en
clair) reste lisible et est chiffrée à sa prochaine modification.

La clé provient, dans l'ordre : de `lesourire.chiffrement.cle`
(`LESOURIRE_CHIFFREMENT_CLE`, base64) ; sinon du fichier indiqué par
`lesourire.chiffrement.fichier` (`fichiers/cle-chiffrement.key` par défaut) ;
sinon elle est générée à la première exécution dans ce fichier. **Sauvegardez ce
fichier avec la base de données** : sans lui, les données chiffrées sont
définitivement illisibles (une sauvegarde de la base seule ne suffit pas).

Comme le nom, le prénom et les téléphones sont chiffrés, la recherche patient /
utilisateur / facture ne se fait plus en SQL mais **en mémoire**, sur les
valeurs déchiffrées (volumes d'un cabinet : largement suffisant).

### Notifications patients (SMS, WhatsApp, e-mail)

Au Cameroun le SMS est le canal réellement lu : c'est donc lui qui part
**automatiquement**. Le fournisseur retenu est l'API **SMS Cameroon 2.0**
d'Orange (`developer.orange.com`), qui couvre tous les opérateurs (MTN et
Orange) et dont les crédits s'achètent par paquets, payés par Orange Money ou
avec le crédit d'une carte SIM Orange.

Le parcours est le suivant :

1. à l'enregistrement d'un rendez-vous, une **confirmation** part vers le
   patient (moins d'une minute après, le temps d'une passe du planificateur) ;
2. **la veille à J-2** (paramètre `rappel.jours_avant_rdv`), un rappel est
   envoyé ; un rendez-vous annulé ou déplacé annule automatiquement les envois
   encore en attente ;
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
fait ou de l'annuler.

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

## Feuille de route

| Phase | Contenu                                                            | État |
|-------|--------------------------------------------------------------------|------|
| 1     | Squelette : BD complète, serveur, client, connexion, navigation    | ✔    |
| 2     | Module Patients (fiche, recherche, tiers payants, audit)           | ✔    |
| 3     | Agenda / RDV + programmation des rappels J-2 (envoi mail/WA ensuite) | ✔    |
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
