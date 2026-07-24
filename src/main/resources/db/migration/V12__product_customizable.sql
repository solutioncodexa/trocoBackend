ALTER TABLE products
    ADD COLUMN IF NOT EXISTS customizable BOOLEAN NOT NULL DEFAULT FALSE;

-- Produits déjà présentés comme personnalisables (logo client)
UPDATE products
SET customizable = TRUE
WHERE deleted = FALSE
  AND (
    LOWER(COALESCE(name, '')) ~ 'personnalis|votre logo|avec logo'
    OR LOWER(COALESCE(description, '')) ~ 'personnalis|votre logo|impression de votre logo|avec logo'
    OR LOWER(COALESCE(short_description, '')) ~ 'personnalis|votre logo|avec logo'
  );
