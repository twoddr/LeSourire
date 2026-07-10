-- ============================================================================
-- V5 - Stock : mise à jour automatique de article.quantite_stock
--      + catégories d'articles initiales
-- ============================================================================

-- Confort de saisie : la date des paiements et mouvements est celle du moment
-- si elle n'est pas fournie explicitement.
ALTER TABLE paiement MODIFY date_paiement DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE mouvement_stock MODIFY date_mouvement DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ENTREE augmente le stock ; AJUSTEMENT est signé (positif ou négatif) ;
-- SORTIE et PEREMPTION diminuent (quantité saisie positive).
CREATE TRIGGER trg_mouvement_stock_after_insert
AFTER INSERT ON mouvement_stock
FOR EACH ROW
BEGIN
    DECLARE v_delta DECIMAL(12,2);

    SET v_delta = CASE
        WHEN NEW.type = 'ENTREE' THEN NEW.quantite
        WHEN NEW.type = 'AJUSTEMENT' THEN NEW.quantite
        ELSE -NEW.quantite
    END;

    UPDATE article
    SET quantite_stock = quantite_stock + v_delta
    WHERE id = NEW.fk_article;
END;

-- La suppression d'un mouvement annule son effet sur le stock
CREATE TRIGGER trg_mouvement_stock_after_delete
AFTER DELETE ON mouvement_stock
FOR EACH ROW
BEGIN
    DECLARE v_delta DECIMAL(12,2);

    SET v_delta = CASE
        WHEN OLD.type = 'ENTREE' THEN OLD.quantite
        WHEN OLD.type = 'AJUSTEMENT' THEN OLD.quantite
        ELSE -OLD.quantite
    END;

    UPDATE article
    SET quantite_stock = quantite_stock - v_delta
    WHERE id = OLD.fk_article;
END;

-- Catégories d'articles initiales (inspirées du fichier de stock du cabinet)
INSERT INTO categorie_article (libelle) VALUES
    ('Consommables de soins'),
    ('Endodontie'),
    ('Composites et restauration'),
    ('Prothèses et laboratoire'),
    ('Hygiène et protection'),
    ('Instruments'),
    ('Médicaments et anesthésie'),
    ('Fournitures de bureau');
