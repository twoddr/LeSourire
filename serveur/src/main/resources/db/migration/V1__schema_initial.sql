-- ============================================================================
-- LE SOURIRE - Schéma initial de la base de données
-- ----------------------------------------------------------------------------
-- Toutes les relations entre tables sont posées dès le départ.
-- Conventions :
--   * noms de tables et colonnes en français, snake_case
--   * montants en DECIMAL(12,2) (devise du cabinet : XAF)
--   * pourcentages en DECIMAL(5,2) (0.00 à 100.00)
--   * clés étrangères préfixées fk_
--   * cree_le / modifie_le gérés par la base
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. UTILISATEURS ET SÉCURITÉ
-- ----------------------------------------------------------------------------

CREATE TABLE utilisateur (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    nom_utilisateur VARCHAR(50)     NOT NULL,
    mot_de_passe    VARCHAR(255)    NOT NULL,           -- hash (encodeur délégué Spring)
    nom             VARCHAR(100)    NOT NULL,
    prenom          VARCHAR(100)    NULL,
    role            VARCHAR(20)     NOT NULL,           -- DENTISTE, ASSISTANT, SECRETAIRE, COMPTABLE, ADMINISTRATEUR
    email           VARCHAR(255)    NULL,
    telephone       VARCHAR(30)     NULL,
    actif           BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_utilisateur_nom_utilisateur (nom_utilisateur),
    CONSTRAINT ck_utilisateur_role CHECK (role IN
        ('DENTISTE', 'ASSISTANT', 'SECRETAIRE', 'COMPTABLE', 'ADMINISTRATEUR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Journal d'audit : qui a fait quoi, quand (important en contexte médical)
CREATE TABLE audit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_utilisateur  BIGINT          NULL,
    date_action     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action          VARCHAR(50)     NOT NULL,           -- CREATION, MODIFICATION, SUPPRESSION, CONNEXION...
    entite          VARCHAR(50)     NOT NULL,           -- nom de la table concernée
    entite_id       BIGINT          NULL,
    details         TEXT            NULL,
    PRIMARY KEY (id),
    KEY idx_audit_entite (entite, entite_id),
    KEY idx_audit_date (date_action),
    CONSTRAINT fk_audit_utilisateur FOREIGN KEY (fk_utilisateur) REFERENCES utilisateur (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Paramètres de l'application (coordonnées du cabinet, SMTP, délais de rappel...)
CREATE TABLE parametre (
    cle             VARCHAR(100)    NOT NULL,
    valeur          TEXT            NULL,
    description     VARCHAR(255)    NULL,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. TIERS PAYANTS (assureurs et sociétés conventionnées)
-- ----------------------------------------------------------------------------

CREATE TABLE assureur (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    nom                 VARCHAR(255)    NOT NULL,
    telephone           VARCHAR(30)     NULL,
    email               VARCHAR(255)    NULL,
    adresse             VARCHAR(255)    NULL,
    pourcentage_defaut  DECIMAL(5,2)    NOT NULL DEFAULT 0,  -- % de prise en charge par défaut
    notes               TEXT            NULL,
    actif               BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_assureur_nom (nom)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE societe (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    nom                 VARCHAR(255)    NOT NULL,
    telephone           VARCHAR(30)     NULL,
    email               VARCHAR(255)    NULL,
    adresse             VARCHAR(255)    NULL,
    pourcentage_defaut  DECIMAL(5,2)    NOT NULL DEFAULT 0,  -- % de prise en charge par défaut
    notes               TEXT            NULL,
    actif               BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_societe_nom (nom)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. PATIENTS
-- ----------------------------------------------------------------------------

CREATE TABLE patient (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    numero_dossier          VARCHAR(20)     NOT NULL,       -- généré par l'application
    nom                     VARCHAR(150)    NOT NULL,
    prenom                  VARCHAR(150)    NULL,
    date_naissance          DATE            NULL,
    sexe                    CHAR(1)         NULL,           -- M / F
    telephone               VARCHAR(30)     NULL,
    telephone_whatsapp      VARCHAR(30)     NULL,           -- si différent du téléphone principal
    email                   VARCHAR(255)    NULL,
    adresse                 VARCHAR(255)    NULL,
    quartier                VARCHAR(150)    NULL,
    ville                   VARCHAR(150)    NULL,
    profession              VARCHAR(150)    NULL,
    personne_urgence_nom    VARCHAR(150)    NULL,
    personne_urgence_tel    VARCHAR(30)     NULL,
    antecedents             TEXT            NULL,           -- antécédents médicaux
    allergies               TEXT            NULL,
    notes                   TEXT            NULL,
    -- Prise en charge : les pourcentages du patient priment sur les défauts
    -- de l'assureur / la société ; ils sont recopiés sur chaque facture émise.
    fk_assureur             BIGINT          NULL,
    numero_assure           VARCHAR(50)     NULL,
    pourcentage_assureur    DECIMAL(5,2)    NULL,           -- NULL = utiliser le défaut de l'assureur
    fk_societe              BIGINT          NULL,
    pourcentage_societe     DECIMAL(5,2)    NULL,           -- NULL = utiliser le défaut de la société
    mauvais_payeur          BOOLEAN         NOT NULL DEFAULT FALSE,
    actif                   BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_par                BIGINT          NULL,
    cree_le                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_patient_numero_dossier (numero_dossier),
    KEY idx_patient_nom (nom, prenom),
    KEY idx_patient_telephone (telephone),
    CONSTRAINT fk_patient_assureur FOREIGN KEY (fk_assureur) REFERENCES assureur (id),
    CONSTRAINT fk_patient_societe FOREIGN KEY (fk_societe) REFERENCES societe (id),
    CONSTRAINT fk_patient_createur FOREIGN KEY (cree_par) REFERENCES utilisateur (id),
    CONSTRAINT ck_patient_sexe CHECK (sexe IN ('M', 'F'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. NOMENCLATURE DES ACTES (tarification par lettre-clé Z / D)
-- ----------------------------------------------------------------------------

-- Les lettres-clés du tarifaire (D et Z, extensible)
CREATE TABLE lettre_cle (
    code            VARCHAR(5)      NOT NULL,           -- 'D', 'Z'
    libelle         VARCHAR(150)    NOT NULL,
    PRIMARY KEY (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Valeur monétaire d'une lettre-clé, versionnée dans le temps :
-- si la valeur du D change, on clôt la période courante et on en ouvre une autre,
-- sans casser l'historique des actes déjà facturés.
CREATE TABLE valeur_lettre_cle (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_lettre_cle   VARCHAR(5)      NOT NULL,
    valeur          DECIMAL(12,2)   NOT NULL,
    date_debut      DATE            NOT NULL,
    date_fin        DATE            NULL,               -- NULL = valeur en vigueur
    PRIMARY KEY (id),
    UNIQUE KEY uq_valeur_lettre_debut (fk_lettre_cle, date_debut),
    CONSTRAINT fk_valeur_lettre_cle FOREIGN KEY (fk_lettre_cle) REFERENCES lettre_cle (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE categorie_prestation (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    libelle         VARCHAR(150)    NOT NULL,
    ordre_affichage INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categorie_prestation_libelle (libelle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Une prestation est tarifée SOIT en lettre-clé x coefficient (ex. D12),
-- SOIT au forfait (ex. consultation 15 000 XAF).
CREATE TABLE prestation (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)     NOT NULL,           -- code court interne (ex. EXT-SIMPLE)
    libelle         VARCHAR(255)    NOT NULL,
    fk_categorie    BIGINT          NOT NULL,
    fk_lettre_cle   VARCHAR(5)      NULL,               -- NULL si forfait
    coefficient     DECIMAL(8,2)    NULL,               -- ex. 12 pour D12
    tarif_forfait   DECIMAL(12,2)   NULL,               -- NULL si lettre-clé
    notes           VARCHAR(255)    NULL,
    actif           BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_prestation_code (code),
    CONSTRAINT fk_prestation_categorie FOREIGN KEY (fk_categorie) REFERENCES categorie_prestation (id),
    CONSTRAINT fk_prestation_lettre_cle FOREIGN KEY (fk_lettre_cle) REFERENCES lettre_cle (code),
    CONSTRAINT ck_prestation_tarification CHECK (
        (fk_lettre_cle IS NOT NULL AND coefficient IS NOT NULL)
        OR tarif_forfait IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. RENDEZ-VOUS ET RAPPELS
-- ----------------------------------------------------------------------------

CREATE TABLE rdv (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_patient      BIGINT          NOT NULL,
    fk_praticien    BIGINT          NOT NULL,           -- utilisateur (dentiste)
    debut           DATETIME        NOT NULL,
    fin             DATETIME        NOT NULL,
    type            VARCHAR(30)     NOT NULL DEFAULT 'CONSULTATION',
    statut          VARCHAR(30)     NOT NULL DEFAULT 'PLANIFIE',
    motif           VARCHAR(255)    NULL,
    fk_acte_origine BIGINT          NULL,               -- pour une revisite : l'acte qui la motive (FK ajoutée plus bas)
    notes           TEXT            NULL,
    cree_par        BIGINT          NULL,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rdv_debut (debut),
    KEY idx_rdv_patient (fk_patient),
    KEY idx_rdv_praticien_debut (fk_praticien, debut),
    CONSTRAINT fk_rdv_patient FOREIGN KEY (fk_patient) REFERENCES patient (id),
    CONSTRAINT fk_rdv_praticien FOREIGN KEY (fk_praticien) REFERENCES utilisateur (id),
    CONSTRAINT fk_rdv_createur FOREIGN KEY (cree_par) REFERENCES utilisateur (id),
    CONSTRAINT ck_rdv_type CHECK (type IN
        ('CONSULTATION', 'SOIN', 'CONTROLE', 'REVISITE', 'URGENCE')),
    CONSTRAINT ck_rdv_statut CHECK (statut IN
        ('PLANIFIE', 'CONFIRME', 'EN_SALLE_ATTENTE', 'HONORE', 'ANNULE', 'ABSENT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Rappels programmés (J-2 avant RDV, revisites post-intervention).
-- Le planificateur du serveur balaie cette table et envoie ce qui est dû.
CREATE TABLE rappel (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_patient      BIGINT          NOT NULL,
    fk_rdv          BIGINT          NULL,               -- rendez-vous concerné, si applicable
    type            VARCHAR(30)     NOT NULL,           -- RAPPEL_RDV, REVISITE
    canal           VARCHAR(20)     NOT NULL,           -- EMAIL, WHATSAPP, SMS
    date_prevue     DATETIME        NOT NULL,           -- quand le rappel doit partir
    date_envoi      DATETIME        NULL,
    statut          VARCHAR(20)     NOT NULL DEFAULT 'EN_ATTENTE',
    destinataire    VARCHAR(255)    NULL,               -- adresse mail ou numéro utilisé
    contenu         TEXT            NULL,
    message_erreur  TEXT            NULL,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rappel_a_envoyer (statut, date_prevue),
    CONSTRAINT fk_rappel_patient FOREIGN KEY (fk_patient) REFERENCES patient (id),
    CONSTRAINT fk_rappel_rdv FOREIGN KEY (fk_rdv) REFERENCES rdv (id),
    CONSTRAINT ck_rappel_type CHECK (type IN ('RAPPEL_RDV', 'REVISITE')),
    CONSTRAINT ck_rappel_canal CHECK (canal IN ('EMAIL', 'WHATSAPP', 'SMS')),
    CONSTRAINT ck_rappel_statut CHECK (statut IN ('EN_ATTENTE', 'ENVOYE', 'ECHEC', 'ANNULE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. ACTES RÉALISÉS (fiche de soins)
-- ----------------------------------------------------------------------------

-- Un acte = une prestation réalisée sur un patient à une date donnée.
-- Le montant est figé au moment de l'acte (coefficient et valeur de la lettre
-- recopiés), pour que l'historique reste juste si le tarifaire évolue.
CREATE TABLE acte (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    fk_patient              BIGINT          NOT NULL,
    fk_praticien            BIGINT          NOT NULL,
    fk_rdv                  BIGINT          NULL,
    fk_prestation           BIGINT          NOT NULL,
    date_acte               DATETIME        NOT NULL,
    dents                   VARCHAR(100)    NULL,       -- numéros FDI, ex. "16" ou "11,21"
    quantite                INT             NOT NULL DEFAULT 1,
    coefficient_applique    DECIMAL(8,2)    NULL,       -- copie au moment de l'acte
    valeur_lettre_appliquee DECIMAL(12,2)   NULL,       -- copie au moment de l'acte
    montant                 DECIMAL(12,2)   NOT NULL,   -- montant total de l'acte
    observations            TEXT            NULL,
    cree_par                BIGINT          NULL,
    cree_le                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_acte_patient_date (fk_patient, date_acte),
    CONSTRAINT fk_acte_patient FOREIGN KEY (fk_patient) REFERENCES patient (id),
    CONSTRAINT fk_acte_praticien FOREIGN KEY (fk_praticien) REFERENCES utilisateur (id),
    CONSTRAINT fk_acte_rdv FOREIGN KEY (fk_rdv) REFERENCES rdv (id),
    CONSTRAINT fk_acte_prestation FOREIGN KEY (fk_prestation) REFERENCES prestation (id),
    CONSTRAINT fk_acte_createur FOREIGN KEY (cree_par) REFERENCES utilisateur (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Lien différé : une revisite (rdv) peut pointer vers l'acte qui la motive
ALTER TABLE rdv
    ADD CONSTRAINT fk_rdv_acte_origine FOREIGN KEY (fk_acte_origine) REFERENCES acte (id);

-- ----------------------------------------------------------------------------
-- 7. FACTURATION ET PAIEMENTS
-- ----------------------------------------------------------------------------

CREATE TABLE facture (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    numero                  VARCHAR(30)     NOT NULL,   -- ex. FA-2026-0001, généré par l'application
    fk_patient              BIGINT          NOT NULL,
    date_facture            DATE            NOT NULL,
    date_echeance           DATE            NULL,
    -- Montants
    montant_brut            DECIMAL(12,2)   NOT NULL DEFAULT 0,    -- somme des lignes
    remise                  DECIMAL(12,2)   NOT NULL DEFAULT 0,    -- réduction accordée
    montant_net             DECIMAL(12,2)   NOT NULL DEFAULT 0,    -- brut - remise
    -- Répartition tiers payants : pourcentages figés au moment de la facture
    fk_assureur             BIGINT          NULL,
    pourcentage_assureur    DECIMAL(5,2)    NOT NULL DEFAULT 0,
    quote_assureur          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    fk_societe              BIGINT          NULL,
    pourcentage_societe     DECIMAL(5,2)    NOT NULL DEFAULT 0,
    quote_societe           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    quote_patient           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    statut                  VARCHAR(30)     NOT NULL DEFAULT 'BROUILLON',
    notes                   TEXT            NULL,
    cree_par                BIGINT          NULL,
    cree_le                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_facture_numero (numero),
    KEY idx_facture_patient (fk_patient),
    KEY idx_facture_date (date_facture),
    KEY idx_facture_statut (statut),
    CONSTRAINT fk_facture_patient FOREIGN KEY (fk_patient) REFERENCES patient (id),
    CONSTRAINT fk_facture_assureur FOREIGN KEY (fk_assureur) REFERENCES assureur (id),
    CONSTRAINT fk_facture_societe FOREIGN KEY (fk_societe) REFERENCES societe (id),
    CONSTRAINT fk_facture_createur FOREIGN KEY (cree_par) REFERENCES utilisateur (id),
    CONSTRAINT ck_facture_statut CHECK (statut IN
        ('BROUILLON', 'EMISE', 'PARTIELLEMENT_PAYEE', 'PAYEE', 'ANNULEE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE facture_ligne (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_facture      BIGINT          NOT NULL,
    fk_acte         BIGINT          NULL,               -- l'acte facturé (NULL pour une ligne libre)
    designation     VARCHAR(255)    NOT NULL,
    quantite        INT             NOT NULL DEFAULT 1,
    prix_unitaire   DECIMAL(12,2)   NOT NULL,
    montant         DECIMAL(12,2)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_facture_ligne_facture (fk_facture),
    CONSTRAINT fk_facture_ligne_facture FOREIGN KEY (fk_facture) REFERENCES facture (id) ON DELETE CASCADE,
    CONSTRAINT fk_facture_ligne_acte FOREIGN KEY (fk_acte) REFERENCES acte (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE paiement (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_facture      BIGINT          NOT NULL,
    date_paiement   DATETIME        NOT NULL,
    montant         DECIMAL(12,2)   NOT NULL,
    mode            VARCHAR(20)     NOT NULL,           -- ESPECES, CHEQUE, VIREMENT, MOBILE_MONEY, CARTE
    payeur          VARCHAR(20)     NOT NULL DEFAULT 'PATIENT',  -- PATIENT, ASSUREUR, SOCIETE
    reference       VARCHAR(100)    NULL,               -- n° de chèque, référence de virement...
    recu_par        BIGINT          NULL,               -- utilisateur ayant encaissé
    notes           TEXT            NULL,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_paiement_facture (fk_facture),
    KEY idx_paiement_date (date_paiement),
    CONSTRAINT fk_paiement_facture FOREIGN KEY (fk_facture) REFERENCES facture (id),
    CONSTRAINT fk_paiement_receveur FOREIGN KEY (recu_par) REFERENCES utilisateur (id),
    CONSTRAINT ck_paiement_mode CHECK (mode IN
        ('ESPECES', 'CHEQUE', 'VIREMENT', 'MOBILE_MONEY', 'CARTE')),
    CONSTRAINT ck_paiement_payeur CHECK (payeur IN ('PATIENT', 'ASSUREUR', 'SOCIETE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. STOCK
-- ----------------------------------------------------------------------------

CREATE TABLE fournisseur (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(255)    NOT NULL,
    contact         VARCHAR(255)    NULL,
    telephone       VARCHAR(30)     NULL,
    email           VARCHAR(255)    NULL,
    adresse         VARCHAR(255)    NULL,
    notes           TEXT            NULL,
    actif           BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_fournisseur_nom (nom)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE categorie_article (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    libelle         VARCHAR(150)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categorie_article_libelle (libelle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE article (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    nom                 VARCHAR(255)    NOT NULL,
    marque              VARCHAR(150)    NULL,
    fk_categorie        BIGINT          NULL,
    unite               VARCHAR(30)     NOT NULL DEFAULT 'unité',   -- boîte, tube, paquet...
    quantite_stock      DECIMAL(12,2)   NOT NULL DEFAULT 0,         -- tenue à jour par les mouvements
    seuil_alerte        DECIMAL(12,2)   NOT NULL DEFAULT 0,         -- alerte si stock <= seuil
    prix_achat_dernier  DECIMAL(12,2)   NULL,
    notes               TEXT            NULL,
    actif               BOOLEAN         NOT NULL DEFAULT TRUE,
    cree_le             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_le          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_article_nom (nom),
    CONSTRAINT fk_article_categorie FOREIGN KEY (fk_categorie) REFERENCES categorie_article (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE mouvement_stock (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    fk_article      BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,           -- ENTREE, SORTIE, AJUSTEMENT, PEREMPTION
    quantite        DECIMAL(12,2)   NOT NULL,           -- toujours positive ; le type donne le sens
    prix_unitaire   DECIMAL(12,2)   NULL,               -- pour les entrées (achats)
    fk_fournisseur  BIGINT          NULL,
    date_mouvement  DATETIME        NOT NULL,
    date_peremption DATE            NULL,               -- pour les lots entrants
    reference       VARCHAR(100)    NULL,               -- n° de commande, bon de livraison...
    fk_utilisateur  BIGINT          NULL,
    notes           TEXT            NULL,
    cree_le         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_mouvement_article_date (fk_article, date_mouvement),
    CONSTRAINT fk_mouvement_article FOREIGN KEY (fk_article) REFERENCES article (id),
    CONSTRAINT fk_mouvement_fournisseur FOREIGN KEY (fk_fournisseur) REFERENCES fournisseur (id),
    CONSTRAINT fk_mouvement_utilisateur FOREIGN KEY (fk_utilisateur) REFERENCES utilisateur (id),
    CONSTRAINT ck_mouvement_type CHECK (type IN
        ('ENTREE', 'SORTIE', 'AJUSTEMENT', 'PEREMPTION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
