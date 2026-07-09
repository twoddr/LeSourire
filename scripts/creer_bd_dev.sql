-- Prépare une MariaDB locale pour le développement.
-- Usage : sudo mariadb < scripts/creer_bd_dev.sql
CREATE DATABASE IF NOT EXISTS lesourire CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lesourire'@'localhost' IDENTIFIED BY 'lesourire';
CREATE USER IF NOT EXISTS 'lesourire'@'%' IDENTIFIED BY 'lesourire';
GRANT ALL PRIVILEGES ON lesourire.* TO 'lesourire'@'localhost';
GRANT ALL PRIVILEGES ON lesourire.* TO 'lesourire'@'%';
FLUSH PRIVILEGES;
