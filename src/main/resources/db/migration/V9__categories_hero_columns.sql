-- Colonnes hero catégories (si absentes sur Neon / BDD existante)

ALTER TABLE categories ADD COLUMN IF NOT EXISTS hero_image_url VARCHAR(1024);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS hero_sort_order INTEGER;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS show_on_hero BOOLEAN;

UPDATE categories SET show_on_hero = FALSE WHERE show_on_hero IS NULL;
ALTER TABLE categories ALTER COLUMN show_on_hero SET DEFAULT FALSE;
