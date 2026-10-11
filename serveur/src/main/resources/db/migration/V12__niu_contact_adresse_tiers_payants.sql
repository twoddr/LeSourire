-- ============================================================================
-- V12 - Tiers payants : NIU, personne de contact et adresse
-- ----------------------------------------------------------------------------
-- Au Cameroun, sociétés et assureurs disposent d'un Numéro d'Identifiant
-- Unique (NIU). On l'ajoute au référentiel des tiers payants, ainsi que le nom
-- de la personne de contact à joindre au cabinet.
--
-- La colonne `adresse` existait déjà (V1) mais n'était pas exposée par
-- l'application : elle devient éditable et consultable sans changement de
-- schéma.
--
-- Les valeurs sont facultatives : les lignes existantes restent valides.
-- ============================================================================

ALTER TABLE assureur
    ADD COLUMN niu         VARCHAR(30)  NULL AFTER nom,
    ADD COLUMN contact_nom VARCHAR(255) NULL AFTER niu;

ALTER TABLE societe
    ADD COLUMN niu         VARCHAR(30)  NULL AFTER nom,
    ADD COLUMN contact_nom VARCHAR(255) NULL AFTER niu;
