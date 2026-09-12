-- ============================================================================
-- V8 - Chiffrement au repos des données sensibles
-- ----------------------------------------------------------------------------
-- L'application chiffre désormais (AES-256-GCM) l'identité et les données
-- médicales des patients, l'identité des utilisateurs et le n° d'assuré.
-- Les colonnes concernées sont élargies pour accueillir le cryptogramme en
-- base64 (préfixe "enc:v1:" + IV + tag), soit environ 1,4 fois la taille du
-- clair plus 40 octets.
--
-- Les index sur les colonnes chiffrées (nom, prénom, téléphone) sont supprimés :
-- ils n'ont plus de sens (le cryptogramme est aléatoire) et dépasseraient la
-- limite InnoDB une fois les colonnes élargies. Le tri et la recherche se font
-- désormais en mémoire, sur les valeurs déchiffrées.
--
-- Les valeurs existantes (en clair) restent lisibles : elles seront chiffrées
-- automatiquement à leur prochaine modification.
-- ============================================================================

-- Les vues exposent les colonnes modifiées : on les recrée à l'identique après.
DROP VIEW IF EXISTS v_facture_relance;
DROP VIEW IF EXISTS v_couverture_active;

-- Suppression des index avant élargissement (limite de taille de clé InnoDB).
ALTER TABLE patient
    DROP INDEX idx_patient_nom,
    DROP INDEX idx_patient_telephone;

ALTER TABLE patient
    MODIFY nom                  VARCHAR(512) NOT NULL,
    MODIFY prenom               VARCHAR(512) NULL,
    MODIFY telephone            VARCHAR(255) NULL,
    MODIFY telephone_whatsapp   VARCHAR(255) NULL,
    MODIFY email                VARCHAR(512) NULL,
    MODIFY adresse              VARCHAR(512) NULL,
    MODIFY quartier             VARCHAR(512) NULL,
    MODIFY ville                VARCHAR(512) NULL,
    MODIFY profession           VARCHAR(512) NULL,
    MODIFY personne_urgence_nom VARCHAR(512) NULL,
    MODIFY personne_urgence_tel VARCHAR(255) NULL;

ALTER TABLE utilisateur
    MODIFY nom       VARCHAR(512) NOT NULL,
    MODIFY prenom    VARCHAR(512) NULL,
    MODIFY email     VARCHAR(512) NULL,
    MODIFY telephone VARCHAR(255) NULL;

ALTER TABLE patient_couverture
    MODIFY numero_assure VARCHAR(255) NULL;

ALTER TABLE rappel
    MODIFY destinataire VARCHAR(512) NULL;

ALTER TABLE rdv
    MODIFY motif VARCHAR(512) NULL;

-- Recréation des vues (sans DEFINER, cf. V7).
CREATE VIEW v_facture_relance AS
SELECT
    f.id, f.numero, f.date_facture, f.date_echeance,
    p.nom AS patient_nom, p.prenom AS patient_prenom,
    'PATIENT' AS payeur_type, NULL AS payeur_nom, f.solde_patient AS solde
FROM facture f
JOIN patient p ON p.id = f.fk_patient
WHERE f.solde_patient > 0 AND f.statut NOT IN ('BROUILLON', 'ANNULEE')

UNION ALL

SELECT
    f.id, f.numero, f.date_facture, f.date_echeance,
    p.nom, p.prenom,
    'ASSUREUR', a.nom, f.solde_assureur
FROM facture f
JOIN patient p ON p.id = f.fk_patient
JOIN assureur a ON a.id = f.fk_assureur
WHERE f.solde_assureur > 0 AND f.statut NOT IN ('BROUILLON', 'ANNULEE')

UNION ALL

SELECT
    f.id, f.numero, f.date_facture, f.date_echeance,
    p.nom, p.prenom,
    'SOCIETE', s.nom, f.solde_societe
FROM facture f
JOIN patient p ON p.id = f.fk_patient
JOIN societe s ON s.id = f.fk_societe
WHERE f.solde_societe > 0 AND f.statut NOT IN ('BROUILLON', 'ANNULEE');

CREATE VIEW v_couverture_active AS
SELECT * FROM patient_couverture
WHERE date_debut <= CURDATE() AND (date_fin IS NULL OR date_fin >= CURDATE());
