-- ============================================================================
-- V10 - Notifications aux patients (SMS / WhatsApp)
-- ----------------------------------------------------------------------------
-- Le cabinet notifie ses patients sur leur téléphone. L'e-mail étant très
-- rarement utilisé au Cameroun, le canal par défaut devient le SMS (API
-- Orange Cameroun), avec WhatsApp en repli (clic assisté « click-to-chat »)
-- et l'e-mail en dernier recours.
--
-- 1. Préférence de canal par patient (+ consentement au rappel).
-- 2. Nouveau type de rappel CONFIRMATION_RDV : envoyé sans délai dès la prise
--    de rendez-vous, contrairement à RAPPEL_RDV (J-2).
-- 3. Paramètres de notification (activation, fournisseur, indicatif, expéditeur).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Préférence de canal de notification par patient
-- ----------------------------------------------------------------------------
-- AUTO     : SMS si un téléphone est renseigné, sinon WhatsApp, sinon e-mail
-- SMS      : envoi automatique via l'API de l'opérateur
-- WHATSAPP : envoi assisté (le secrétariat clique sur le lien wa.me)
-- EMAIL    : envoi assisté (lien mailto:, sans compte SMTP à configurer)
ALTER TABLE patient
    ADD COLUMN canal_notification VARCHAR(20) NOT NULL DEFAULT 'AUTO'
        COMMENT 'AUTO, SMS, WHATSAPP, EMAIL' AFTER email,
    ADD COLUMN consentement_rappel BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Le patient accepte de recevoir les rappels par téléphone'
        AFTER canal_notification,
    ADD CONSTRAINT ck_patient_canal_notification
        CHECK (canal_notification IN ('AUTO', 'SMS', 'WHATSAPP', 'EMAIL'));

-- ----------------------------------------------------------------------------
-- 2. Confirmation immédiate à la prise de rendez-vous
-- ----------------------------------------------------------------------------
ALTER TABLE rappel DROP CONSTRAINT ck_rappel_type;

ALTER TABLE rappel
    ADD CONSTRAINT ck_rappel_type CHECK (type IN ('CONFIRMATION_RDV', 'RAPPEL_RDV', 'REVISITE'));

-- Compteur de tentatives : un envoi qui échoue (réseau coupé, crédit épuisé)
-- est retenté par le planificateur jusqu'à « notification.max_tentatives ».
ALTER TABLE rappel
    ADD COLUMN tentatives INT NOT NULL DEFAULT 0
        COMMENT 'Tentatives d''envoi déjà effectuées' AFTER statut;

-- ----------------------------------------------------------------------------
-- 3. Paramètres de notification
-- ----------------------------------------------------------------------------
-- Les identifiants de l'API Orange (Client ID / Secret) ne sont PAS stockés en
-- base : ils sont fournis par l'environnement du serveur
-- (LESOURIRE_ORANGE_CLIENT_ID / LESOURIRE_ORANGE_CLIENT_SECRET).
INSERT INTO parametre (cle, valeur, description) VALUES
    ('notification.active',          'false',               'Activer l''envoi automatique des notifications (true/false)'),
    ('notification.fournisseur',     'journal',             'Fournisseur SMS : orange (réel) ou journal (journalisé, test)'),
    ('notification.indicatif_pays',  '+237',                'Indicatif téléphonique du pays (Cameroun)'),
    ('notification.nom_expediteur',  'CABINET LE SOURIRE',  'Nom d''expéditeur des SMS (whitelist opérateur)'),
    ('notification.max_tentatives',  '3',                   'Nombre maximal de tentatives d''envoi d''un rappel');
