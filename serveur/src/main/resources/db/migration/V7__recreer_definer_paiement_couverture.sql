-- ============================================================================
-- V7 - Recréer procédure, triggers et vues créés avec DEFINER='lesourire'@'localhost'.
-- Sans cet utilisateur MySQL, SELECT sur v_facture_relance (et encaissements
-- via trg_paiement_*) échouent avec : "definer does not exist".
-- ============================================================================

DROP TRIGGER IF EXISTS trg_paiement_after_insert;
DROP TRIGGER IF EXISTS trg_paiement_after_update;
DROP TRIGGER IF EXISTS trg_paiement_after_delete;
DROP PROCEDURE IF EXISTS sp_recalculer_facture;

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

DROP VIEW IF EXISTS v_facture_relance;
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

DROP TRIGGER IF EXISTS trg_couverture_check_chevauchement;
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

DROP VIEW IF EXISTS v_couverture_active;
CREATE VIEW v_couverture_active AS
SELECT * FROM patient_couverture
WHERE date_debut <= CURDATE() AND (date_fin IS NULL OR date_fin >= CURDATE());
