-- ============================================================================
-- V4 - Historique des couvertures du patient (assureur / société)
-- ----------------------------------------------------------------------------
-- Remplace patient.fk_assureur / fk_societe (couverture "actuelle" seulement)
-- par un historique versionné, sur le même principe que valeur_lettre_cle.
-- Un patient peut avoir 1 assureur ET 1 société simultanément, mais pas
-- 2 couvertures actives du même type.
-- ============================================================================

CREATE TABLE patient_couverture (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    fk_patient          BIGINT          NOT NULL,
    type                VARCHAR(20)     NOT NULL,       -- ASSUREUR, SOCIETE
    fk_assureur         BIGINT          NULL,
    fk_societe          BIGINT          NULL,
    numero_assure       VARCHAR(50)     NULL,
    pourcentage         DECIMAL(5,2)    NULL,           -- NULL = utiliser le défaut du payeur
    date_debut          DATE            NOT NULL,
    date_fin            DATE            NULL,           -- NULL = couverture en cours
    motif_fin           VARCHAR(255)    NULL,           -- ex. "changement d'employeur"
    cree_par            BIGINT          NULL,
    cree_le             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_couverture_patient_dates (fk_patient, date_debut, date_fin),
    CONSTRAINT fk_couverture_patient FOREIGN KEY (fk_patient) REFERENCES patient (id),
    CONSTRAINT fk_couverture_assureur FOREIGN KEY (fk_assureur) REFERENCES assureur (id),
    CONSTRAINT fk_couverture_societe FOREIGN KEY (fk_societe) REFERENCES societe (id),
    CONSTRAINT fk_couverture_createur FOREIGN KEY (cree_par) REFERENCES utilisateur (id),
    CONSTRAINT ck_couverture_type CHECK (type IN ('ASSUREUR', 'SOCIETE')),
    CONSTRAINT ck_couverture_cible CHECK (
        (type = 'ASSUREUR' AND fk_assureur IS NOT NULL AND fk_societe IS NULL)
        OR (type = 'SOCIETE' AND fk_societe IS NOT NULL AND fk_assureur IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Empêche le chevauchement de deux couvertures du même type pour un patient.
-- Ne couvre que l'INSERT : l'application ne fait jamais d'UPDATE des dates,
-- hormis la clôture (pose de date_fin), qui ne crée pas de chevauchement.
CREATE TRIGGER trg_couverture_check_chevauchement
BEFORE INSERT ON patient_couverture
FOR EACH ROW
BEGIN
    DECLARE v_conflits INT;
    SELECT COUNT(*) INTO v_conflits
    FROM patient_couverture
    WHERE fk_patient = NEW.fk_patient
      AND type = NEW.type
      AND (date_fin IS NULL OR date_fin >= NEW.date_debut)
      AND (NEW.date_fin IS NULL OR NEW.date_fin >= date_debut);

    IF v_conflits > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Chevauchement de couverture pour ce patient/type sur cette période';
    END IF;
END;

-- Migration des couvertures existantes depuis les colonnes du patient
INSERT INTO patient_couverture (fk_patient, type, fk_assureur, numero_assure, pourcentage, date_debut)
SELECT id, 'ASSUREUR', fk_assureur, numero_assure, pourcentage_assureur, DATE(cree_le)
FROM patient WHERE fk_assureur IS NOT NULL;

INSERT INTO patient_couverture (fk_patient, type, fk_societe, pourcentage, date_debut)
SELECT id, 'SOCIETE', fk_societe, pourcentage_societe, DATE(cree_le)
FROM patient WHERE fk_societe IS NOT NULL;

-- Suppression des colonnes désormais obsolètes
ALTER TABLE patient
    DROP FOREIGN KEY fk_patient_assureur,
    DROP FOREIGN KEY fk_patient_societe,
    DROP COLUMN fk_assureur,
    DROP COLUMN numero_assure,
    DROP COLUMN pourcentage_assureur,
    DROP COLUMN fk_societe,
    DROP COLUMN pourcentage_societe;

-- Vue pratique : couvertures actives à ce jour
CREATE VIEW v_couverture_active AS
SELECT * FROM patient_couverture
WHERE date_debut <= CURDATE() AND (date_fin IS NULL OR date_fin >= CURDATE());
