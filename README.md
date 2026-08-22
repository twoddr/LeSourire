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

## Prérequis (développement)

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
java -jar serveur/target/lesourire-serveur-0.1.0-SNAPSHOT.jar

# 4. Lancer le client
mvn -pl client javafx:run
```

Compte initial : `admin` / `admin` (**à changer** dès que le module
Administration sera actif). L'écran de connexion propose aussi un **mode
démonstration** qui présente l'interface sans serveur.

Configuration du serveur par variables d'environnement :
`LESOURIRE_BD_URL`, `LESOURIRE_BD_UTILISATEUR`, `LESOURIRE_BD_MOT_DE_PASSE`,
`LESOURIRE_PORT` (défaut : `8420`).

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
  d'articles initiales.

Toute évolution du schéma = un nouveau fichier `V<n>__description.sql`,
appliqué automatiquement chez le client à la mise à jour du serveur.

Pour créer la base **sans passer par le serveur** (import direct MariaDB) :
`scripts/lesourire_complet.sql` contient tout le schéma V1→V5, les données
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
| 6b    | Tableau de bord enrichi + comptabilité / envoi des rappels          |      |
| 7     | Installeur Windows (jpackage/MSI), service Windows, mises à jour    |      |

## Déploiement Windows (bêta)

Pack « zip + double-clic », sans MSI pour l’instant.

```bash
# Depuis la machine de build (Linux OK) :
./scripts/preparer_deploiement.sh
# ou, si vous avez déjà un JDK 21 Windows sous la main :
./scripts/preparer_deploiement.sh /chemin/vers/java-21-windows
```

Résultat : `dist/LeSourire/` contenant serveur, client (JavaFX win), scripts
`.bat`, SQL de première install et (optionnel) le runtime Java.

Sur le PC du cabinet : MariaDB + coller `java-21` dans `LeSourire\java\` si
besoin + `sql\01_creer_bd.sql` une fois + `Demarrer-LeSourire.bat`.
Détails : `deploy/modele/LISEZ-MOI.txt` (copié dans le pack).

Plus tard (phase 7) : MSI via `jpackage`, service Windows, mises à jour auto.
