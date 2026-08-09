-- Catégories : activation / désactivation (soft hide vitrine)
-- Étapes séparées pour compat PostgreSQL + Hibernate ddl-auto (évite NOT NULL immédiat sur table remplie).

ALTER TABLE categories ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE categories SET active = TRUE WHERE active IS NULL;

ALTER TABLE categories ALTER COLUMN active SET DEFAULT TRUE;

DO $$
BEGIN
  ALTER TABLE categories ALTER COLUMN active SET NOT NULL;
EXCEPTION
  WHEN others THEN
    NULL; -- déjà NOT NULL ou verrou schema
END $$;
