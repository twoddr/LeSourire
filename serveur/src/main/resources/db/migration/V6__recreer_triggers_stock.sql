-- ============================================================================
-- V6 - Recréer les triggers stock avec le DEFINER courant.
-- Les triggers V5 avaient été créés avec DEFINER='lesourire'@'localhost'.
-- Si cet utilisateur MySQL n'existe plus, tout INSERT sur mouvement_stock
-- échoue avec : "The user specified as a definer does not exist".
-- ============================================================================

DROP TRIGGER IF EXISTS trg_mouvement_stock_after_insert;
DROP TRIGGER IF EXISTS trg_mouvement_stock_after_delete;

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
