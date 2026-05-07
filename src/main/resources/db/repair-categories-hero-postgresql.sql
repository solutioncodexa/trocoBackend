-- Réparation one-shot (Neon / PostgreSQL) si ddl-auto a échoué sur show_on_hero
-- ou si les colonnes hero manquent. Exécuter dans le SQL editor du projet.

ALTER TABLE categories ADD COLUMN IF NOT EXISTS hero_image_url varchar(1024);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS hero_sort_order integer;
UPDATE categories SET hero_sort_order = 0 WHERE hero_sort_order IS NULL;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS show_on_hero boolean;
UPDATE categories SET show_on_hero = false WHERE show_on_hero IS NULL;
ALTER TABLE categories ALTER COLUMN show_on_hero SET DEFAULT false;
ALTER TABLE categories ALTER COLUMN show_on_hero SET NOT NULL;
