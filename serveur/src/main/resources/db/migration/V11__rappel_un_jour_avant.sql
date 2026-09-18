-- ============================================================================
-- V11 - Rappel patient la veille (J-1) au lieu de J-2
-- ----------------------------------------------------------------------------
-- Le cabinet rappelle désormais ses patients la veille du rendez-vous : le
-- délai par défaut passe de 2 à 1 jour. Le parcours reste celui-ci :
--   1. confirmation envoyée sans délai à la prise du rendez-vous ;
--   2. rappel envoyé la veille (J-1).
--
-- La valeur reste modifiable à tout moment depuis Administration → Paramètres
-- (clé `rappel.jours_avant_rdv`), y compris pour revenir à 2 jours : ce script
-- n'aligne que les bases déjà installées sur le nouveau réglage du cabinet.
--
-- NB : les commentaires des migrations V1 et V10 qui parlent encore de « J-2 »
-- sont laissés tels quels à dessein — un script déjà appliqué ne doit jamais
-- être retouché, sinon Flyway refuse de démarrer (contrôle des sommes de
-- contrôle enregistrées dans flyway_schema_history).
-- ============================================================================

UPDATE parametre
   SET valeur = '1'
 WHERE cle = 'rappel.jours_avant_rdv';
