-- ============================================================================
-- LE SOURIRE - Base de données complète (schéma V1 à V5 + données initiales)
-- ----------------------------------------------------------------------------
-- Import :   mariadb -u root -p < lesourire_complet.sql
--            (le fichier crée lui-même la base `lesourire`)
-- Droits :   CREATE USER IF NOT EXISTS 'lesourire'@'localhost' IDENTIFIED BY 'lesourire';
--            GRANT ALL PRIVILEGES ON lesourire.* TO 'lesourire'@'localhost';
--
-- Ce dump contient la table flyway_schema_history avec les sommes de contrôle
-- des migrations V1 à V5 : le serveur démarrera dessus sans rien rejouer, et
-- les futures migrations (V6, ...) s'appliqueront automatiquement.
--
-- Compte applicatif initial : admin / admin (à changer dès la 1re connexion).
-- ============================================================================

/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-12.3.2-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: lesourire
-- ------------------------------------------------------
-- Server version	12.3.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Current Database: `lesourire`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `lesourire` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `lesourire`;

--
-- Table structure for table `acte`
--

DROP TABLE IF EXISTS `acte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `acte` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_patient` bigint(20) NOT NULL,
  `fk_praticien` bigint(20) NOT NULL,
  `fk_rdv` bigint(20) DEFAULT NULL,
  `fk_prestation` bigint(20) NOT NULL,
  `date_acte` datetime NOT NULL,
  `dents` varchar(100) DEFAULT NULL,
  `quantite` int(11) NOT NULL DEFAULT 1,
  `coefficient_applique` decimal(8,2) DEFAULT NULL,
  `valeur_lettre_appliquee` decimal(12,2) DEFAULT NULL,
  `montant` decimal(12,2) NOT NULL,
  `observations` text DEFAULT NULL,
  `cree_par` bigint(20) DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_acte_patient_date` (`fk_patient`,`date_acte`),
  KEY `fk_acte_praticien` (`fk_praticien`),
  KEY `fk_acte_rdv` (`fk_rdv`),
  KEY `fk_acte_prestation` (`fk_prestation`),
  KEY `fk_acte_createur` (`cree_par`),
  CONSTRAINT `fk_acte_createur` FOREIGN KEY (`cree_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `fk_acte_patient` FOREIGN KEY (`fk_patient`) REFERENCES `patient` (`id`),
  CONSTRAINT `fk_acte_praticien` FOREIGN KEY (`fk_praticien`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `fk_acte_prestation` FOREIGN KEY (`fk_prestation`) REFERENCES `prestation` (`id`),
  CONSTRAINT `fk_acte_rdv` FOREIGN KEY (`fk_rdv`) REFERENCES `rdv` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acte`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `acte` WRITE;
/*!40000 ALTER TABLE `acte` DISABLE KEYS */;
/*!40000 ALTER TABLE `acte` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `article`
--

DROP TABLE IF EXISTS `article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `article` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `marque` varchar(150) DEFAULT NULL,
  `fk_categorie` bigint(20) DEFAULT NULL,
  `unite` varchar(30) NOT NULL DEFAULT 'unité',
  `quantite_stock` decimal(12,2) NOT NULL DEFAULT 0.00,
  `seuil_alerte` decimal(12,2) NOT NULL DEFAULT 0.00,
  `prix_achat_dernier` decimal(12,2) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_article_nom` (`nom`),
  KEY `fk_article_categorie` (`fk_categorie`),
  CONSTRAINT `fk_article_categorie` FOREIGN KEY (`fk_categorie`) REFERENCES `categorie_article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `article`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `article` WRITE;
/*!40000 ALTER TABLE `article` DISABLE KEYS */;
/*!40000 ALTER TABLE `article` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `assureur`
--

DROP TABLE IF EXISTS `assureur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `assureur` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `telephone` varchar(30) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `pourcentage_defaut` decimal(5,2) NOT NULL DEFAULT 0.00,
  `notes` text DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_assureur_nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assureur`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `assureur` WRITE;
/*!40000 ALTER TABLE `assureur` DISABLE KEYS */;
/*!40000 ALTER TABLE `assureur` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_utilisateur` bigint(20) DEFAULT NULL,
  `date_action` datetime NOT NULL DEFAULT current_timestamp(),
  `action` varchar(50) NOT NULL,
  `entite` varchar(50) NOT NULL,
  `entite_id` bigint(20) DEFAULT NULL,
  `details` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_entite` (`entite`,`entite_id`),
  KEY `idx_audit_date` (`date_action`),
  KEY `fk_audit_utilisateur` (`fk_utilisateur`),
  CONSTRAINT `fk_audit_utilisateur` FOREIGN KEY (`fk_utilisateur`) REFERENCES `utilisateur` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `categorie_article`
--

DROP TABLE IF EXISTS `categorie_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorie_article` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `libelle` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_categorie_article_libelle` (`libelle`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categorie_article`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `categorie_article` WRITE;
/*!40000 ALTER TABLE `categorie_article` DISABLE KEYS */;
INSERT INTO `categorie_article` VALUES
(3,'Composites et restauration'),
(1,'Consommables de soins'),
(2,'Endodontie'),
(8,'Fournitures de bureau'),
(5,'Hygiène et protection'),
(6,'Instruments'),
(7,'Médicaments et anesthésie'),
(4,'Prothèses et laboratoire');
/*!40000 ALTER TABLE `categorie_article` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `categorie_prestation`
--

DROP TABLE IF EXISTS `categorie_prestation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorie_prestation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `libelle` varchar(150) NOT NULL,
  `ordre_affichage` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_categorie_prestation_libelle` (`libelle`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categorie_prestation`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `categorie_prestation` WRITE;
/*!40000 ALTER TABLE `categorie_prestation` DISABLE KEYS */;
INSERT INTO `categorie_prestation` VALUES
(1,'Consultation',1),
(2,'Soins conservateurs',2),
(3,'Soins chirurgicaux',3),
(4,'Prothèses dentaires',4),
(5,'Radio diagnostique',5),
(6,'Traitement orthodontique',6);
/*!40000 ALTER TABLE `categorie_prestation` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `facture`
--

DROP TABLE IF EXISTS `facture`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `facture` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `numero` varchar(30) NOT NULL,
  `fk_patient` bigint(20) NOT NULL,
  `date_facture` date NOT NULL,
  `date_echeance` date DEFAULT NULL,
  `montant_brut` decimal(12,2) NOT NULL DEFAULT 0.00,
  `remise` decimal(12,2) NOT NULL DEFAULT 0.00,
  `montant_net` decimal(12,2) NOT NULL DEFAULT 0.00,
  `fk_assureur` bigint(20) DEFAULT NULL,
  `pourcentage_assureur` decimal(5,2) NOT NULL DEFAULT 0.00,
  `quote_assureur` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paye_assureur` decimal(12,2) NOT NULL DEFAULT 0.00,
  `solde_assureur` decimal(12,2) GENERATED ALWAYS AS (`quote_assureur` - `paye_assureur`) STORED,
  `fk_societe` bigint(20) DEFAULT NULL,
  `pourcentage_societe` decimal(5,2) NOT NULL DEFAULT 0.00,
  `quote_societe` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paye_societe` decimal(12,2) NOT NULL DEFAULT 0.00,
  `solde_societe` decimal(12,2) GENERATED ALWAYS AS (`quote_societe` - `paye_societe`) STORED,
  `quote_patient` decimal(12,2) NOT NULL DEFAULT 0.00,
  `paye_patient` decimal(12,2) NOT NULL DEFAULT 0.00,
  `solde_patient` decimal(12,2) GENERATED ALWAYS AS (`quote_patient` - `paye_patient`) STORED,
  `statut` varchar(30) NOT NULL DEFAULT 'BROUILLON',
  `notes` text DEFAULT NULL,
  `cree_par` bigint(20) DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_facture_numero` (`numero`),
  KEY `idx_facture_patient` (`fk_patient`),
  KEY `idx_facture_date` (`date_facture`),
  KEY `idx_facture_statut` (`statut`),
  KEY `fk_facture_assureur` (`fk_assureur`),
  KEY `fk_facture_societe` (`fk_societe`),
  KEY `fk_facture_createur` (`cree_par`),
  CONSTRAINT `fk_facture_assureur` FOREIGN KEY (`fk_assureur`) REFERENCES `assureur` (`id`),
  CONSTRAINT `fk_facture_createur` FOREIGN KEY (`cree_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `fk_facture_patient` FOREIGN KEY (`fk_patient`) REFERENCES `patient` (`id`),
  CONSTRAINT `fk_facture_societe` FOREIGN KEY (`fk_societe`) REFERENCES `societe` (`id`),
  CONSTRAINT `ck_facture_statut` CHECK (`statut` in ('BROUILLON','EMISE','PARTIELLEMENT_PAYEE','PAYEE','ANNULEE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `facture`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `facture` WRITE;
/*!40000 ALTER TABLE `facture` DISABLE KEYS */;
/*!40000 ALTER TABLE `facture` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `facture_ligne`
--

DROP TABLE IF EXISTS `facture_ligne`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `facture_ligne` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_facture` bigint(20) NOT NULL,
  `fk_acte` bigint(20) DEFAULT NULL,
  `designation` varchar(255) NOT NULL,
  `quantite` int(11) NOT NULL DEFAULT 1,
  `prix_unitaire` decimal(12,2) NOT NULL,
  `montant` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_facture_ligne_facture` (`fk_facture`),
  KEY `fk_facture_ligne_acte` (`fk_acte`),
  CONSTRAINT `fk_facture_ligne_acte` FOREIGN KEY (`fk_acte`) REFERENCES `acte` (`id`),
  CONSTRAINT `fk_facture_ligne_facture` FOREIGN KEY (`fk_facture`) REFERENCES `facture` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `facture_ligne`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `facture_ligne` WRITE;
/*!40000 ALTER TABLE `facture_ligne` DISABLE KEYS */;
/*!40000 ALTER TABLE `facture_ligne` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES
(1,'1','schema initial','SQL','V1__schema_initial.sql',1773260874,'lesourire','2026-07-10 12:11:26',45,1),
(2,'2','donnees initiales','SQL','V2__donnees_initiales.sql',974152615,'lesourire','2026-07-10 12:11:26',20,1),
(3,'3','suivi paiements par payeur','SQL','V3__suivi_paiements_par_payeur.sql',136848372,'lesourire','2026-07-10 12:11:26',38,1),
(4,'4','historique couverture patient','SQL','V4__historique_couverture_patient.sql',1617480746,'lesourire','2026-07-10 12:11:26',15,1),
(5,'5','triggers stock et categories','SQL','V5__triggers_stock_et_categories.sql',-760687993,'lesourire','2026-07-10 12:11:26',13,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `fournisseur`
--

DROP TABLE IF EXISTS `fournisseur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `fournisseur` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `contact` varchar(255) DEFAULT NULL,
  `telephone` varchar(30) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_fournisseur_nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fournisseur`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `fournisseur` WRITE;
/*!40000 ALTER TABLE `fournisseur` DISABLE KEYS */;
/*!40000 ALTER TABLE `fournisseur` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `lettre_cle`
--

DROP TABLE IF EXISTS `lettre_cle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `lettre_cle` (
  `code` varchar(5) NOT NULL,
  `libelle` varchar(150) NOT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lettre_cle`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `lettre_cle` WRITE;
/*!40000 ALTER TABLE `lettre_cle` DISABLE KEYS */;
INSERT INTO `lettre_cle` VALUES
('D','Actes de soins dentaires'),
('Z','Actes de radiologie');
/*!40000 ALTER TABLE `lettre_cle` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `mouvement_stock`
--

DROP TABLE IF EXISTS `mouvement_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `mouvement_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_article` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL,
  `quantite` decimal(12,2) NOT NULL,
  `prix_unitaire` decimal(12,2) DEFAULT NULL,
  `fk_fournisseur` bigint(20) DEFAULT NULL,
  `date_mouvement` datetime NOT NULL DEFAULT current_timestamp(),
  `date_peremption` date DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  `fk_utilisateur` bigint(20) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_mouvement_article_date` (`fk_article`,`date_mouvement`),
  KEY `fk_mouvement_fournisseur` (`fk_fournisseur`),
  KEY `fk_mouvement_utilisateur` (`fk_utilisateur`),
  CONSTRAINT `fk_mouvement_article` FOREIGN KEY (`fk_article`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_mouvement_fournisseur` FOREIGN KEY (`fk_fournisseur`) REFERENCES `fournisseur` (`id`),
  CONSTRAINT `fk_mouvement_utilisateur` FOREIGN KEY (`fk_utilisateur`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `ck_mouvement_type` CHECK (`type` in ('ENTREE','SORTIE','AJUSTEMENT','PEREMPTION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mouvement_stock`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `mouvement_stock` WRITE;
/*!40000 ALTER TABLE `mouvement_stock` DISABLE KEYS */;
/*!40000 ALTER TABLE `mouvement_stock` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_mouvement_stock_after_insert
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
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_mouvement_stock_after_delete
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
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `paiement`
--

DROP TABLE IF EXISTS `paiement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `paiement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_facture` bigint(20) NOT NULL,
  `date_paiement` datetime NOT NULL DEFAULT current_timestamp(),
  `montant` decimal(12,2) NOT NULL,
  `mode` varchar(20) NOT NULL,
  `payeur` varchar(20) NOT NULL DEFAULT 'PATIENT',
  `reference` varchar(100) DEFAULT NULL,
  `recu_par` bigint(20) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_paiement_facture` (`fk_facture`),
  KEY `idx_paiement_date` (`date_paiement`),
  KEY `fk_paiement_receveur` (`recu_par`),
  CONSTRAINT `fk_paiement_facture` FOREIGN KEY (`fk_facture`) REFERENCES `facture` (`id`),
  CONSTRAINT `fk_paiement_receveur` FOREIGN KEY (`recu_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `ck_paiement_mode` CHECK (`mode` in ('ESPECES','CHEQUE','VIREMENT','MOBILE_MONEY','CARTE')),
  CONSTRAINT `ck_paiement_payeur` CHECK (`payeur` in ('PATIENT','ASSUREUR','SOCIETE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `paiement`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `paiement` WRITE;
/*!40000 ALTER TABLE `paiement` DISABLE KEYS */;
/*!40000 ALTER TABLE `paiement` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_paiement_after_insert
AFTER INSERT ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(NEW.fk_facture);
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_paiement_after_update
AFTER UPDATE ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(NEW.fk_facture);
    IF OLD.fk_facture <> NEW.fk_facture THEN
        CALL sp_recalculer_facture(OLD.fk_facture);
    END IF;
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_paiement_after_delete
AFTER DELETE ON paiement
FOR EACH ROW
BEGIN
    CALL sp_recalculer_facture(OLD.fk_facture);
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `parametre`
--

DROP TABLE IF EXISTS `parametre`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `parametre` (
  `cle` varchar(100) NOT NULL,
  `valeur` text DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`cle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parametre`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `parametre` WRITE;
/*!40000 ALTER TABLE `parametre` DISABLE KEYS */;
INSERT INTO `parametre` VALUES
('cabinet.adresse','100 Rue Dikoumé Bell, Bali, BP 4302 Douala, Cameroun','Adresse du cabinet','2026-07-10 14:11:26'),
('cabinet.devise','XAF','Devise (franc CFA)','2026-07-10 14:11:26'),
('cabinet.email','','Adresse mail du cabinet','2026-07-10 14:11:26'),
('cabinet.fuseau_horaire','Africa/Douala','Fuseau horaire','2026-07-10 14:11:26'),
('cabinet.nom','Cabinet Dentaire Le Sourire','Nom affiché sur les documents','2026-07-10 14:11:26'),
('cabinet.praticien','Docteur Nadine TOWE','Praticien principal','2026-07-10 14:11:26'),
('cabinet.telephone','(237) 233 431 411','Téléphone du cabinet','2026-07-10 14:11:26'),
('rappel.heure_envoi','09:00','Heure d\'envoi des rappels du jour','2026-07-10 14:11:26'),
('rappel.jours_avant_rdv','2','Nombre de jours avant RDV pour le rappel','2026-07-10 14:11:26'),
('sauvegarde.dossier','sauvegardes','Dossier des sauvegardes de la BD','2026-07-10 14:11:26'),
('sauvegarde.heure','22:00','Heure de la sauvegarde quotidienne','2026-07-10 14:11:26'),
('smtp.hote','','Serveur SMTP pour les mails','2026-07-10 14:11:26'),
('smtp.mot_de_passe','','Mot de passe SMTP','2026-07-10 14:11:26'),
('smtp.port','587','Port SMTP','2026-07-10 14:11:26'),
('smtp.utilisateur','','Compte SMTP','2026-07-10 14:11:26');
/*!40000 ALTER TABLE `parametre` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `patient`
--

DROP TABLE IF EXISTS `patient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `numero_dossier` varchar(20) NOT NULL,
  `nom` varchar(150) NOT NULL,
  `prenom` varchar(150) DEFAULT NULL,
  `date_naissance` date DEFAULT NULL,
  `sexe` char(1) DEFAULT NULL,
  `telephone` varchar(30) DEFAULT NULL,
  `telephone_whatsapp` varchar(30) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `quartier` varchar(150) DEFAULT NULL,
  `ville` varchar(150) DEFAULT NULL,
  `profession` varchar(150) DEFAULT NULL,
  `personne_urgence_nom` varchar(150) DEFAULT NULL,
  `personne_urgence_tel` varchar(30) DEFAULT NULL,
  `antecedents` text DEFAULT NULL,
  `allergies` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `mauvais_payeur` tinyint(1) NOT NULL DEFAULT 0,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_par` bigint(20) DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_patient_numero_dossier` (`numero_dossier`),
  KEY `idx_patient_nom` (`nom`,`prenom`),
  KEY `idx_patient_telephone` (`telephone`),
  KEY `fk_patient_createur` (`cree_par`),
  CONSTRAINT `fk_patient_createur` FOREIGN KEY (`cree_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `ck_patient_sexe` CHECK (`sexe` in ('M','F'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `patient_couverture`
--

DROP TABLE IF EXISTS `patient_couverture`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient_couverture` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_patient` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL,
  `fk_assureur` bigint(20) DEFAULT NULL,
  `fk_societe` bigint(20) DEFAULT NULL,
  `numero_assure` varchar(50) DEFAULT NULL,
  `pourcentage` decimal(5,2) DEFAULT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date DEFAULT NULL,
  `motif_fin` varchar(255) DEFAULT NULL,
  `cree_par` bigint(20) DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_couverture_patient_dates` (`fk_patient`,`date_debut`,`date_fin`),
  KEY `fk_couverture_assureur` (`fk_assureur`),
  KEY `fk_couverture_societe` (`fk_societe`),
  KEY `fk_couverture_createur` (`cree_par`),
  CONSTRAINT `fk_couverture_assureur` FOREIGN KEY (`fk_assureur`) REFERENCES `assureur` (`id`),
  CONSTRAINT `fk_couverture_createur` FOREIGN KEY (`cree_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `fk_couverture_patient` FOREIGN KEY (`fk_patient`) REFERENCES `patient` (`id`),
  CONSTRAINT `fk_couverture_societe` FOREIGN KEY (`fk_societe`) REFERENCES `societe` (`id`),
  CONSTRAINT `ck_couverture_type` CHECK (`type` in ('ASSUREUR','SOCIETE')),
  CONSTRAINT `ck_couverture_cible` CHECK (`type` = 'ASSUREUR' and `fk_assureur` is not null and `fk_societe` is null or `type` = 'SOCIETE' and `fk_societe` is not null and `fk_assureur` is null)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_couverture`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `patient_couverture` WRITE;
/*!40000 ALTER TABLE `patient_couverture` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_couverture` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`lesourire`@`localhost`*/ /*!50003 TRIGGER trg_couverture_check_chevauchement
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
END 
*/;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `prestation`
--

DROP TABLE IF EXISTS `prestation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `prestation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  `libelle` varchar(255) NOT NULL,
  `fk_categorie` bigint(20) NOT NULL,
  `fk_lettre_cle` varchar(5) DEFAULT NULL,
  `coefficient` decimal(8,2) DEFAULT NULL,
  `tarif_forfait` decimal(12,2) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_prestation_code` (`code`),
  KEY `fk_prestation_categorie` (`fk_categorie`),
  KEY `fk_prestation_lettre_cle` (`fk_lettre_cle`),
  CONSTRAINT `fk_prestation_categorie` FOREIGN KEY (`fk_categorie`) REFERENCES `categorie_prestation` (`id`),
  CONSTRAINT `fk_prestation_lettre_cle` FOREIGN KEY (`fk_lettre_cle`) REFERENCES `lettre_cle` (`code`),
  CONSTRAINT `ck_prestation_tarification` CHECK (`fk_lettre_cle` is not null and `coefficient` is not null or `tarif_forfait` is not null)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prestation`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `prestation` WRITE;
/*!40000 ALTER TABLE `prestation` DISABLE KEYS */;
INSERT INTO `prestation` VALUES
(1,'CONS-JOUR','Consultation de jour',1,NULL,NULL,15000.00,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(2,'CONS-NUIT','Consultation de nuit',1,NULL,NULL,20000.00,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(3,'CAV-2F','Cavité composée (2 faces)',2,'D',12.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(4,'CAV-3F','Cavité composée (3 faces)',2,'D',15.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(5,'PULP-IC','Pulpectomie incisivo-canine',2,'D',10.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(6,'PULP-PM','Pulpectomie prémolaire',2,'D',15.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(7,'PULP-MOL','Pulpectomie groupe molaire',2,'D',25.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(8,'RECON-BASE','Reconstitution (base)',2,'D',6.00,NULL,'Tarifaire : D6 + D18/D30 selon le cas',1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(9,'RECON-18','Reconstitution (complément D18)',2,'D',18.00,NULL,'Tarifaire : D6 + D18/D30 selon le cas',1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(10,'RECON-30','Reconstitution (complément D30)',2,'D',30.00,NULL,'Tarifaire : D6 + D18/D30 selon le cas',1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(11,'EXT-SIMPLE','Extraction d\'une dent',3,'D',10.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(12,'EXT-MALPOS','Extraction d\'une dent en malposition',3,'D',20.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(13,'EXT-INCL','Extraction d\'une dent incluse ou enclavée',3,'D',40.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(14,'RX-INTRA','Examen intra-buccal',5,'Z',4.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(15,'RX-EXTRA','Examen extra-buccal',5,'Z',16.00,NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(16,'ORTHO-MA','Traitement par multi-attaches (par semestre)',6,NULL,NULL,500000.00,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26'),
(17,'ORTHO-INT','Traitement interceptif',6,NULL,NULL,300000.00,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26');
/*!40000 ALTER TABLE `prestation` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `rappel`
--

DROP TABLE IF EXISTS `rappel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rappel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_patient` bigint(20) NOT NULL,
  `fk_rdv` bigint(20) DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `canal` varchar(20) NOT NULL,
  `date_prevue` datetime NOT NULL,
  `date_envoi` datetime DEFAULT NULL,
  `statut` varchar(20) NOT NULL DEFAULT 'EN_ATTENTE',
  `destinataire` varchar(255) DEFAULT NULL,
  `contenu` text DEFAULT NULL,
  `message_erreur` text DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_rappel_a_envoyer` (`statut`,`date_prevue`),
  KEY `fk_rappel_patient` (`fk_patient`),
  KEY `fk_rappel_rdv` (`fk_rdv`),
  CONSTRAINT `fk_rappel_patient` FOREIGN KEY (`fk_patient`) REFERENCES `patient` (`id`),
  CONSTRAINT `fk_rappel_rdv` FOREIGN KEY (`fk_rdv`) REFERENCES `rdv` (`id`),
  CONSTRAINT `ck_rappel_type` CHECK (`type` in ('RAPPEL_RDV','REVISITE')),
  CONSTRAINT `ck_rappel_canal` CHECK (`canal` in ('EMAIL','WHATSAPP','SMS')),
  CONSTRAINT `ck_rappel_statut` CHECK (`statut` in ('EN_ATTENTE','ENVOYE','ECHEC','ANNULE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rappel`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `rappel` WRITE;
/*!40000 ALTER TABLE `rappel` DISABLE KEYS */;
/*!40000 ALTER TABLE `rappel` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `rdv`
--

DROP TABLE IF EXISTS `rdv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rdv` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_patient` bigint(20) NOT NULL,
  `fk_praticien` bigint(20) NOT NULL,
  `debut` datetime NOT NULL,
  `fin` datetime NOT NULL,
  `type` varchar(30) NOT NULL DEFAULT 'CONSULTATION',
  `statut` varchar(30) NOT NULL DEFAULT 'PLANIFIE',
  `motif` varchar(255) DEFAULT NULL,
  `fk_acte_origine` bigint(20) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `cree_par` bigint(20) DEFAULT NULL,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_rdv_debut` (`debut`),
  KEY `idx_rdv_patient` (`fk_patient`),
  KEY `idx_rdv_praticien_debut` (`fk_praticien`,`debut`),
  KEY `fk_rdv_createur` (`cree_par`),
  KEY `fk_rdv_acte_origine` (`fk_acte_origine`),
  CONSTRAINT `fk_rdv_acte_origine` FOREIGN KEY (`fk_acte_origine`) REFERENCES `acte` (`id`),
  CONSTRAINT `fk_rdv_createur` FOREIGN KEY (`cree_par`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `fk_rdv_patient` FOREIGN KEY (`fk_patient`) REFERENCES `patient` (`id`),
  CONSTRAINT `fk_rdv_praticien` FOREIGN KEY (`fk_praticien`) REFERENCES `utilisateur` (`id`),
  CONSTRAINT `ck_rdv_type` CHECK (`type` in ('CONSULTATION','SOIN','CONTROLE','REVISITE','URGENCE')),
  CONSTRAINT `ck_rdv_statut` CHECK (`statut` in ('PLANIFIE','CONFIRME','EN_SALLE_ATTENTE','HONORE','ANNULE','ABSENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rdv`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `rdv` WRITE;
/*!40000 ALTER TABLE `rdv` DISABLE KEYS */;
/*!40000 ALTER TABLE `rdv` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `societe`
--

DROP TABLE IF EXISTS `societe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `societe` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `telephone` varchar(30) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `pourcentage_defaut` decimal(5,2) NOT NULL DEFAULT 0.00,
  `notes` text DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_societe_nom` (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `societe`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `societe` WRITE;
/*!40000 ALTER TABLE `societe` DISABLE KEYS */;
/*!40000 ALTER TABLE `societe` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `utilisateur`
--

DROP TABLE IF EXISTS `utilisateur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `utilisateur` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nom_utilisateur` varchar(50) NOT NULL,
  `mot_de_passe` varchar(255) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) DEFAULT NULL,
  `role` varchar(20) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `telephone` varchar(30) DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `cree_le` datetime NOT NULL DEFAULT current_timestamp(),
  `modifie_le` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_utilisateur_nom_utilisateur` (`nom_utilisateur`),
  CONSTRAINT `ck_utilisateur_role` CHECK (`role` in ('DENTISTE','ASSISTANT','SECRETAIRE','COMPTABLE','ADMINISTRATEUR'))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utilisateur`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `utilisateur` WRITE;
/*!40000 ALTER TABLE `utilisateur` DISABLE KEYS */;
INSERT INTO `utilisateur` VALUES
(1,'admin','{noop}admin','Administrateur',NULL,'ADMINISTRATEUR',NULL,NULL,1,'2026-07-10 14:11:26','2026-07-10 14:11:26');
/*!40000 ALTER TABLE `utilisateur` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Temporary table structure for view `v_couverture_active`
--

DROP TABLE IF EXISTS `v_couverture_active`;
/*!50001 DROP VIEW IF EXISTS `v_couverture_active`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
/*!50001 CREATE VIEW `v_couverture_active` AS SELECT
 NULL AS `id`,
 NULL AS `fk_patient`,
 NULL AS `type`,
 NULL AS `fk_assureur`,
 NULL AS `fk_societe`,
 NULL AS `numero_assure`,
 NULL AS `pourcentage`,
 NULL AS `date_debut`,
 NULL AS `date_fin`,
 NULL AS `motif_fin`,
 NULL AS `cree_par`,
 NULL AS `cree_le` */;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `v_facture_relance`
--

DROP TABLE IF EXISTS `v_facture_relance`;
/*!50001 DROP VIEW IF EXISTS `v_facture_relance`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
/*!50001 CREATE VIEW `v_facture_relance` AS SELECT
 NULL AS `id`,
 NULL AS `numero`,
 NULL AS `date_facture`,
 NULL AS `date_echeance`,
 NULL AS `patient_nom`,
 NULL AS `patient_prenom`,
 NULL AS `payeur_type`,
 NULL AS `payeur_nom`,
 NULL AS `solde` */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `valeur_lettre_cle`
--

DROP TABLE IF EXISTS `valeur_lettre_cle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `valeur_lettre_cle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fk_lettre_cle` varchar(5) NOT NULL,
  `valeur` decimal(12,2) NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_valeur_lettre_debut` (`fk_lettre_cle`,`date_debut`),
  CONSTRAINT `fk_valeur_lettre_cle` FOREIGN KEY (`fk_lettre_cle`) REFERENCES `lettre_cle` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `valeur_lettre_cle`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `valeur_lettre_cle` WRITE;
/*!40000 ALTER TABLE `valeur_lettre_cle` DISABLE KEYS */;
INSERT INTO `valeur_lettre_cle` VALUES
(1,'D',1200.00,'2000-01-01',NULL),
(2,'Z',1200.00,'2000-01-01',NULL);
/*!40000 ALTER TABLE `valeur_lettre_cle` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Dumping routines for database 'lesourire'
--
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'IGNORE_SPACE,STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_recalculer_facture` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_unicode_ci */ ;
DELIMITER ;;
CREATE DEFINER=`lesourire`@`localhost` PROCEDURE `sp_recalculer_facture`(IN p_fk_facture BIGINT)
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
END
;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Current Database: `lesourire`
--

USE `lesourire`;

--
-- Final view structure for view `v_couverture_active`
--

/*!50001 DROP VIEW IF EXISTS `v_couverture_active`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`lesourire`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_couverture_active` AS select `patient_couverture`.`id` AS `id`,`patient_couverture`.`fk_patient` AS `fk_patient`,`patient_couverture`.`type` AS `type`,`patient_couverture`.`fk_assureur` AS `fk_assureur`,`patient_couverture`.`fk_societe` AS `fk_societe`,`patient_couverture`.`numero_assure` AS `numero_assure`,`patient_couverture`.`pourcentage` AS `pourcentage`,`patient_couverture`.`date_debut` AS `date_debut`,`patient_couverture`.`date_fin` AS `date_fin`,`patient_couverture`.`motif_fin` AS `motif_fin`,`patient_couverture`.`cree_par` AS `cree_par`,`patient_couverture`.`cree_le` AS `cree_le` from `patient_couverture` where `patient_couverture`.`date_debut` <= curdate() and (`patient_couverture`.`date_fin` is null or `patient_couverture`.`date_fin` >= curdate()) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_facture_relance`
--

/*!50001 DROP VIEW IF EXISTS `v_facture_relance`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`lesourire`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_facture_relance` AS select `f`.`id` AS `id`,`f`.`numero` AS `numero`,`f`.`date_facture` AS `date_facture`,`f`.`date_echeance` AS `date_echeance`,`p`.`nom` AS `patient_nom`,`p`.`prenom` AS `patient_prenom`,'PATIENT' AS `payeur_type`,NULL AS `payeur_nom`,`f`.`solde_patient` AS `solde` from (`facture` `f` join `patient` `p` on(`p`.`id` = `f`.`fk_patient`)) where `f`.`solde_patient` > 0 and `f`.`statut` not in ('BROUILLON','ANNULEE') union all select `f`.`id` AS `id`,`f`.`numero` AS `numero`,`f`.`date_facture` AS `date_facture`,`f`.`date_echeance` AS `date_echeance`,`p`.`nom` AS `nom`,`p`.`prenom` AS `prenom`,'ASSUREUR' AS `ASSUREUR`,`a`.`nom` AS `nom`,`f`.`solde_assureur` AS `solde_assureur` from ((`facture` `f` join `patient` `p` on(`p`.`id` = `f`.`fk_patient`)) join `assureur` `a` on(`a`.`id` = `f`.`fk_assureur`)) where `f`.`solde_assureur` > 0 and `f`.`statut` not in ('BROUILLON','ANNULEE') union all select `f`.`id` AS `id`,`f`.`numero` AS `numero`,`f`.`date_facture` AS `date_facture`,`f`.`date_echeance` AS `date_echeance`,`p`.`nom` AS `nom`,`p`.`prenom` AS `prenom`,'SOCIETE' AS `SOCIETE`,`s`.`nom` AS `nom`,`f`.`solde_societe` AS `solde_societe` from ((`facture` `f` join `patient` `p` on(`p`.`id` = `f`.`fk_patient`)) join `societe` `s` on(`s`.`id` = `f`.`fk_societe`)) where `f`.`solde_societe` > 0 and `f`.`statut` not in ('BROUILLON','ANNULEE') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-07-10 14:17:17
