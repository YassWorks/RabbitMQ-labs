CREATE DATABASE ho_db;
CREATE DATABASE bo1_db;
CREATE DATABASE bo2_db;

-- Créer la table dans BO1
USE bo1_db;
CREATE TABLE product_sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_date DATE NOT NULL,
    region VARCHAR(50),
    product VARCHAR(100),
    qty INT,
    cost DECIMAL(10,2),
    amt DECIMAL(10,2),
    tax DECIMAL(10,2),
    total DECIMAL(10,2),
    synced BOOLEAN DEFAULT FALSE   -- flag pour savoir si déjà envoyé
);

-- Créer la table dans BO2
USE bo2_db;
CREATE TABLE product_sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_date DATE NOT NULL,
    region VARCHAR(50),
    product VARCHAR(100),
    qty INT,
    cost DECIMAL(10,2),
    amt DECIMAL(10,2),
    tax DECIMAL(10,2),
    total DECIMAL(10,2),
    synced BOOLEAN DEFAULT FALSE
);

-- Créer la table dans HO (avec colonne source pour savoir d'où vient la ligne)
USE ho_db;
CREATE TABLE product_sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(10), -- 'BO1' ou 'BO2'
    original_id INT,
    sale_date DATE NOT NULL,
    region VARCHAR(50),
    product VARCHAR(100),
    qty INT,
    cost DECIMAL(10,2),
    amt DECIMAL(10,2),
    tax DECIMAL(10,2),
    total DECIMAL(10,2)
);