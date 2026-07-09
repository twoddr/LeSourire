-- ============================================================================
-- LE SOURIRE - Données initiales
-- ----------------------------------------------------------------------------
-- Compte administrateur, lettres-clés et tarifaire officiel du cabinet
-- (source : "LE TARIFAIRE DES SOINS", Cabinet Dentaire Le Sourire, Douala)
-- ============================================================================

-- Compte administrateur initial.
-- Mot de passe temporaire "admin" ({noop} = non hashé) : À CHANGER dès la
-- première connexion ; le module Administration le re-hashera en bcrypt.
INSERT INTO utilisateur (nom_utilisateur, mot_de_passe, nom, prenom, role)
VALUES ('admin', '{noop}admin', 'Administrateur', NULL, 'ADMINISTRATEUR');

-- ----------------------------------------------------------------------------
-- Lettres-clés : D = Z = 1200 francs CFA (tarifaire en vigueur)
-- ----------------------------------------------------------------------------

INSERT INTO lettre_cle (code, libelle) VALUES
    ('D', 'Actes de soins dentaires'),
    ('Z', 'Actes de radiologie');

INSERT INTO valeur_lettre_cle (fk_lettre_cle, valeur, date_debut, date_fin) VALUES
    ('D', 1200.00, '2000-01-01', NULL),
    ('Z', 1200.00, '2000-01-01', NULL);

-- ----------------------------------------------------------------------------
-- Catégories du tarifaire
-- ----------------------------------------------------------------------------

INSERT INTO categorie_prestation (libelle, ordre_affichage) VALUES
    ('Consultation', 1),
    ('Soins conservateurs', 2),
    ('Soins chirurgicaux', 3),
    ('Prothèses dentaires', 4),
    ('Radio diagnostique', 5),
    ('Traitement orthodontique', 6);

-- ----------------------------------------------------------------------------
-- Prestations du tarifaire
-- ----------------------------------------------------------------------------

-- Consultation (forfaits)
INSERT INTO prestation (code, libelle, fk_categorie, fk_lettre_cle, coefficient, tarif_forfait, notes) VALUES
    ('CONS-JOUR',  'Consultation de jour',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Consultation'), NULL, NULL, 15000.00, NULL),
    ('CONS-NUIT',  'Consultation de nuit',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Consultation'), NULL, NULL, 20000.00, NULL);

-- Soins conservateurs (lettre-clé D)
INSERT INTO prestation (code, libelle, fk_categorie, fk_lettre_cle, coefficient, tarif_forfait, notes) VALUES
    ('CAV-2F',     'Cavité composée (2 faces)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 12, NULL, NULL),
    ('CAV-3F',     'Cavité composée (3 faces)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 15, NULL, NULL),
    ('PULP-IC',    'Pulpectomie incisivo-canine',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 10, NULL, NULL),
    ('PULP-PM',    'Pulpectomie prémolaire',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 15, NULL, NULL),
    ('PULP-MOL',   'Pulpectomie groupe molaire',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 25, NULL, NULL),
    ('RECON-BASE', 'Reconstitution (base)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 6, NULL,
        'Tarifaire : D6 + D18/D30 selon le cas'),
    ('RECON-18',   'Reconstitution (complément D18)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 18, NULL,
        'Tarifaire : D6 + D18/D30 selon le cas'),
    ('RECON-30',   'Reconstitution (complément D30)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins conservateurs'), 'D', 30, NULL,
        'Tarifaire : D6 + D18/D30 selon le cas');

-- Soins chirurgicaux (lettre-clé D)
INSERT INTO prestation (code, libelle, fk_categorie, fk_lettre_cle, coefficient, tarif_forfait, notes) VALUES
    ('EXT-SIMPLE', 'Extraction d''une dent',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins chirurgicaux'), 'D', 10, NULL, NULL),
    ('EXT-MALPOS', 'Extraction d''une dent en malposition',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins chirurgicaux'), 'D', 20, NULL, NULL),
    ('EXT-INCL',   'Extraction d''une dent incluse ou enclavée',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Soins chirurgicaux'), 'D', 40, NULL, NULL);

-- Radio diagnostique (lettre-clé Z)
INSERT INTO prestation (code, libelle, fk_categorie, fk_lettre_cle, coefficient, tarif_forfait, notes) VALUES
    ('RX-INTRA',   'Examen intra-buccal',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Radio diagnostique'), 'Z', 4, NULL, NULL),
    ('RX-EXTRA',   'Examen extra-buccal',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Radio diagnostique'), 'Z', 16, NULL, NULL);

-- Traitement orthodontique (forfaits)
INSERT INTO prestation (code, libelle, fk_categorie, fk_lettre_cle, coefficient, tarif_forfait, notes) VALUES
    ('ORTHO-MA',   'Traitement par multi-attaches (par semestre)',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Traitement orthodontique'), NULL, NULL, 500000.00, NULL),
    ('ORTHO-INT',  'Traitement interceptif',
        (SELECT id FROM categorie_prestation WHERE libelle = 'Traitement orthodontique'), NULL, NULL, 300000.00, NULL);

-- Prothèses : codification variable (nombre de dents / type de prothèse),
-- les prestations seront ajoutées via le module Administration.

-- ----------------------------------------------------------------------------
-- Paramètres du cabinet
-- ----------------------------------------------------------------------------

INSERT INTO parametre (cle, valeur, description) VALUES
    ('cabinet.nom',            'Cabinet Dentaire Le Sourire',      'Nom affiché sur les documents'),
    ('cabinet.praticien',      'Docteur Nadine TOWE',              'Praticien principal'),
    ('cabinet.adresse',        '100 Rue Dikoumé Bell, Bali, BP 4302 Douala, Cameroun', 'Adresse du cabinet'),
    ('cabinet.telephone',      '(237) 233 431 411',                'Téléphone du cabinet'),
    ('cabinet.email',          '',                                 'Adresse mail du cabinet'),
    ('cabinet.devise',         'XAF',                              'Devise (franc CFA)'),
    ('cabinet.fuseau_horaire', 'Africa/Douala',                    'Fuseau horaire'),
    ('rappel.jours_avant_rdv', '2',                                'Nombre de jours avant RDV pour le rappel'),
    ('rappel.heure_envoi',     '09:00',                            'Heure d''envoi des rappels du jour'),
    ('smtp.hote',              '',                                 'Serveur SMTP pour les mails'),
    ('smtp.port',              '587',                              'Port SMTP'),
    ('smtp.utilisateur',       '',                                 'Compte SMTP'),
    ('smtp.mot_de_passe',      '',                                 'Mot de passe SMTP'),
    ('sauvegarde.heure',       '22:00',                            'Heure de la sauvegarde quotidienne'),
    ('sauvegarde.dossier',     'sauvegardes',                      'Dossier des sauvegardes de la BD');
