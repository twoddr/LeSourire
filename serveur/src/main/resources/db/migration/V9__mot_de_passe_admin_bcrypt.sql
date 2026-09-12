-- ============================================================================
-- V9 - Mot de passe du compte initial : passage de {noop} à bcrypt
-- ----------------------------------------------------------------------------
-- Le compte admin livré en V2 utilisait "{noop}admin" (mot de passe stocké en
-- clair). On le remplace par son empreinte bcrypt — le mot de passe reste
-- "admin" et doit être changé à la première connexion.
-- Le UPDATE ne cible que la valeur initiale : un mot de passe déjà modifié par
-- le cabinet n'est pas touché.
-- ============================================================================

UPDATE utilisateur
SET mot_de_passe = '{bcrypt}$2b$10$jdmfhXzDWE35lFNPH9DQm.EWccpBQBXePmGtGB6Teqga6u5rMJiei'
WHERE nom_utilisateur = 'admin'
  AND mot_de_passe = '{noop}admin';
