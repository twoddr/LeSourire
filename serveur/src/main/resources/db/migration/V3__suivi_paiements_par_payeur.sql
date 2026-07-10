-- ============================================================================
-- V3 - Suivi des paiements par payeur (patient / assureur / société)
-- ----------------------------------------------------------------------------
-- Les montants payés par payeur sont maintenus automatiquement par triggers
-- à chaque mouvement sur la table paiement ; les soldes sont des colonnes
-- générées, donc impossibles à désynchroniser.
-- (Apport BD revu : consolidation des anciens scripts V3/V5 manuels)
-- ============================================================================

ALTER TABLE facture
    ADD COLUMN paye_patient  DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER quote_patient,
    ADD COLUMN paye_assureur DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER quote_assureur,
    ADD COLUMN paye_societe  DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER quote_societe;

ALTER TABLE facture
    ADD COLUMN solde_patient  DECIMAL(12,2) AS (quote_patient  - paye_patient)  STORED AFTER paye_patient,
    ADD COLUMN solde_assureur DECIMAL(12,2) AS (quote_assureur - paye_assureur) STORED AFTER paye_assureur,
    ADD COLUMN solde_societe  DECIMAL(12,2) AS (quote_societe  - paye_societe)  STORED AFTER paye_societe;

-- Recalcule les montants payés et le statut global d'une facture
CREATE PROCEDURE sp_recalculer_facture(IN p_fk_facture BIGINT)
BEGIN
    DECLARE v_paye_patient, v_paye_assureur, v_paye_societe DECIMAL(12,2);
    DECLARE v_quote_patient, v_quote_assureur, v_quote_societe DECIMAL(12,2);
    DECLARE v_total_du, v_total_paye DECIMAL(12,2);
    DECLARE v_statut VARCHAR(30);

    SELECT
        COALESCE(SUM(CASE WHEN payeur = 'PATIENT'  THEN montant END), 0),
        COALESCE(SUM(CASE WHEN payeur = 'ASSUREUR' THEN montant END), 0),
        COALESCE(SUM(CASE WHEN payeur = 'SOCIETE'  THEN montant END), 0)
    INTO v_paye_patient, v_paye_assureur, v_paye_societe
    FROM paiement
    WHERE fk_facture = p_fk_facture;

    SELECT quote_patient, quote_assureur, quote_societe
    INTO v_quote_patient, v_quote_assureur, v_quote_societe
    FROM facture WHERE id = p_fk_facture;

    SET v_total_du   = v_quote_patient + v_quote_assureur + v_quote_societe;
    SET v_total_paye = v_paye_patient + v_paye_assureur + v_paye_societe;

    SET v_statut = CASE
        WHEN v_total_paye <= 0 THEN 'EMISE'
        WHEN v_total_paye < v_total_du THEN 'PARTIELLEMENT_PAYEE'
        ELSE 'PAYEE'
    END;

    UPDATE facture
    SET paye_patient  = v_paye_patient,
        paye_assureur = v_paye_assureur,
        paye_societe  = v_paye_societe,
        statut = CASE WHEN statut IN ('BROUILLON', 'ANNULEE') THEN statut ELSE v_statut END
    WHERE id = p_fk_facture;
END;

CREATE TRIGGER trg_paiement_after_insert
AFTER INSERT ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(NEW.fk_facture);
END;

CREATE TRIGGER trg_paiement_after_update
AFTER UPDATE ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(NEW.fk_facture);
    IF OLD.fk_facture <> NEW.fk_facture THEN
        CALL sp_recalculer_facture(OLD.fk_facture);
    END IF;
END;

CREATE TRIGGER trg_paiement_after_delete
AFTER DELETE ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(OLD.fk_facture);
END;

-- Vue de reporting : les relances à faire, par payeur
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
