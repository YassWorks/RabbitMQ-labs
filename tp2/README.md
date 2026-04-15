# DB Sync — Synchronisation de bases de données avec RabbitMQ

> **TP2 — Fondement des Systèmes Répartis**  
> Synchronisation distribuée de bases de données MySQL via RabbitMQ Message Queues

---

## Architecture

```
┌─────────────────┐        ┌──────────────────────┐        ┌─────────────────┐
│   BO1 (bo1_db)  │──────▶ │  Queue : bo1.sync     │──────▶│                 │
│   MySQL :3308   │        │  RabbitMQ (Docker)    │        │   HO (ho_db)    │
├─────────────────┤        │  localhost:5672        │        │   MySQL :3308   │
│   BO2 (bo2_db)  │──────▶ │  Queue : bo2.sync     │──────▶│                 │
│   MySQL :3308   │        └──────────────────────┘        └─────────────────┘
└─────────────────┘
     Producers                   RabbitMQ                      Consumer (HO)
```

**Flux :**
1. Chaque Branch Office (BO) lit ses nouvelles ventes depuis MySQL
2. Les envoie en JSON dans sa queue RabbitMQ dédiée
3. Le Head Office (HO) consomme les 2 queues et insère dans sa base centrale

---

## Structure du projet

```
tp2/
├── mysql queries
│   ├── create-db.sql
│   ├── insert-db.sql
├── pom.xml
├── README.md
├── sql/
│   ├── init_bo1.sql
│   ├── init_bo2.sql
│   └── init_ho.sql
└── src/
    └── main/
        └── java/
            └── com/dbsync/
                ├── model/
                │   └── SaleRecord.java
                ├── producer/
                │   └── BOProducer.java
                └── consumer/
                    └── HOConsumer.java
```
---

## ÉTAPE 1 — Lancer RabbitMQ avec Docker

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3-management
```

Vérifier que le container est bien lancé :

```bash
docker ps
```

Accéder à l'interface de management :

```
URL      : http://localhost:15672
Login    : admin
Password : admin
```

> Les queues `bo1.sync` et `bo2.sync` seront créées automatiquement au premier lancement des Producers.

---

## ÉTAPE 2 — Initialiser les bases de données MySQL

Connecte-toi à MySQL (port **3308**, user **root**, password vide) 

### Créer les 3 bases

on importe le fichier `mysql queries/create-db.sql` pour créer les bases de données `bo1_db` , `bo2_db` et `ho_db` ou chaque bd a une table `product_sales`


---

## ÉTAPE 3 — Insérer les données de test

### Données BO1

on importe le fichier `mysql queries/insert-db.sql` pour insérer quelques données non synchronisés 

## ÉTAPE 4 — Configuration du projet Java

### Variables de configuration (à adapter si besoin)

| Variable         | Valeur par défaut           | Fichier          |
|------------------|-----------------------------|------------------|
| `RABBITMQ_HOST`  | `localhost`                 | Producer/Consumer|
| `RABBITMQ_USER`  | `admin`                     | Producer/Consumer|
| `RABBITMQ_PASS`  | `admin`                     | Producer/Consumer|
| `DB_USER`        | `root`                      | Producer/Consumer|
| `DB_PASS`        | `""` (vide)                 | Producer/Consumer|
| MySQL port       | `3308`                      | URL JDBC         |

---

## ÉTAPE 5 — Compilation et lancement

### Compiler

```bash
cd tp2
mvn clean package -q
```

### Terminal 1 — Lancer le Consumer HO (toujours en premier)

```bash
mvn exec:java -Dexec.mainClass="com.dbsync.consumer.HOConsumer"
```

**Sortie attendue :**
```
=== HO Consumer démarré — En attente de messages... ===
  ✔ Écoute de la queue : bo1.sync
  ✔ Écoute de la queue : bo2.sync
En attente... (CTRL+C pour arrêter)
```

### Terminal 2 — Synchroniser BO1

```bash
mvn exec:java \
  -Dexec.mainClass="com.dbsync.producer.BOProducer" \
  -Dexec.args="BO1 bo1_db"
```

**Sortie attendue (Terminal 2) :**
```
=== BO1 Producer démarré ===
Queue  : bo1.sync
Base   : bo1_db
3 enregistrement(s) à envoyer.
  ➤ Envoyé : [BO1] 2024-04-01 | East | Paper | qty=73 | total=1011.52
  ➤ Envoyé : [BO1] 2024-04-02 | East | Pens | qty=14 | total=32.81
  ➤ Envoyé : [BO1] 2024-04-03 | East | Paper | qty=21 | total=290.99
=== BO1 : synchronisation terminée ===
```

**Sortie attendue (Terminal 1 — Consumer) :**
```
  ← Reçu [BO1] : [BO1] 2024-04-01 | East | Paper | qty=73 | total=1011.52
  ✓ Inséré dans HO et acquitté.
  ...
```

### Terminal 3 — Synchroniser BO2

```bash
mvn exec:java \
  -Dexec.mainClass="com.dbsync.producer.BOProducer" \
  -Dexec.args="BO2 bo2_db"
```

---

## Vérification des résultats

### Vérifier la base HO

```sql
USE ho_db;
SELECT * FROM product_sales ORDER BY source, sale_date;
```

**Résultat attendu : 6 lignes** (3 de BO1 + 3 de BO2)

```
+----+--------+-------------+------------+--------+---------+-----+--------+--------+--------+---------+
| id | source | original_id | sale_date  | region | product | qty | cost   | amt    | tax    | total   |
+----+--------+-------------+------------+--------+---------+-----+--------+--------+--------+---------+
|  1 | BO1    |           1 | 2024-04-01 | East   | Paper   |  73 |  12.95 | 945.35 |  66.17 | 1011.52 |
|  2 | BO1    |           2 | 2024-04-02 | East   | Pens    |  14 |   2.19 |  30.66 |   2.15 |   32.81 |
|  3 | BO1    |           3 | 2024-04-03 | East   | Paper   |  21 |  12.95 | 271.95 |  19.04 |  290.99 |
|  4 | BO2    |           1 | 2024-04-01 | West   | Paper   |  33 |  12.95 | 427.35 |  29.91 |  457.26 |
|  5 | BO2    |           2 | 2024-04-02 | West   | Pens    |  40 |   2.19 |  87.60 |   6.13 |   93.73 |
|  6 | BO2    |           3 | 2024-04-03 | West   | Paper   |  10 |  12.95 | 129.50 |   9.07 |  138.57 |
+----+--------+-------------+------------+--------+---------+-----+--------+--------+--------+---------+
```

### Vérifier que les BOs sont marquées synchronisées

```sql
USE bo1_db;
SELECT id, product, synced FROM product_sales;
-- synced doit être 1 (TRUE) pour toutes les lignes

USE bo2_db;
SELECT id, product, synced FROM product_sales;
```

### Vérifier les queues dans RabbitMQ

Ouvrir [http://localhost:15672](http://localhost:15672) → onglet **Queues**

- `bo1.sync` → **0 messages** (tout consommé)
- `bo2.sync` → **0 messages** (tout consommé)

---

## ÉTAPE 6 — Tester la résilience (connexion intermittente)

Ce scénario simule une BO qui envoie des données pendant que le HO est coupé.

```bash
# 1. Arrêter le consumer HO
CTRL+C dans Terminal 1

# 2. Ajouter de nouvelles ventes dans BO1
mysql -h localhost -P 3308 -u root
USE bo1_db;
INSERT INTO product_sales (sale_date, region, product, qty, cost, amt, tax, total)
VALUES ('2024-04-04', 'East', 'Pencils', 50, 1.50, 75.00, 5.25, 80.25);

# 3. Lancer le producer BO1 (les messages s'accumulent dans la queue)
mvn exec:java -Dexec.mainClass="com.dbsync.producer.BOProducer" -Dexec.args="BO1 bo1_db"

# 4. Vérifier dans RabbitMQ UI : bo1.sync a maintenant 1 message en attente

# 5. Relancer le consumer HO → il va traiter les messages en attente
mvn exec:java -Dexec.mainClass="com.dbsync.consumer.HOConsumer"
```

> Grâce aux **queues durables** et aux **messages persistants**, aucune donnée n'est perdue même si le HO est indisponible.

---

### Réinitialiser les flags de synchronisation (pour re-tester)

```sql
-- Remettre tous les enregistrements comme non-synchronisés
USE bo1_db;
UPDATE product_sales SET synced = FALSE;

USE bo2_db;
UPDATE product_sales SET synced = FALSE;

-- Vider la table HO pour repartir propre
USE ho_db;
TRUNCATE TABLE product_sales;
```

---

## Auteur

**Aymen Abid & Mohamed Yassine Kallel** — TP2 Fondement des Systèmes Répartis
