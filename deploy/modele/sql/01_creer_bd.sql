-- Première installation Le Sourire (MariaDB / MySQL).
-- À exécuter UNE FOIS, en root, avant le premier démarrage du serveur.
--
-- Exemple (Windows, MariaDB dans le PATH) :
--   mysql -u root -p < 01_creer_bd.sql
--
-- Ensuite le serveur applique automatiquement les migrations Flyway.

CREATE DATABASE IF NOT EXISTS lesourire
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Compte applicatif (doit correspondre à serveur\config.bat)
CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'csa-soft';
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'csa-soft';
GRANT ALL PRIVILEGES ON lesourire.* TO 'admin'@'localhost';
GRANT ALL PRIVILEGES ON lesourire.* TO 'admin'@'%';

-- Compte « definer » des triggers / procédures (créés historiquement
-- sous ce nom). Sans lui, les inserts sur patient_couverture / paiement plantent.
CREATE USER IF NOT EXISTS 'lesourire'@'localhost' IDENTIFIED BY 'csa-soft';
GRANT ALL PRIVILEGES ON lesourire.* TO 'lesourire'@'localhost';

FLUSH PRIVILEGES;
