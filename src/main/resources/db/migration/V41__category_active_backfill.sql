-- Répare active NULL (Hibernate ddl-auto / démarrages partiels)
UPDATE categories SET active = TRUE WHERE active IS NULL;
