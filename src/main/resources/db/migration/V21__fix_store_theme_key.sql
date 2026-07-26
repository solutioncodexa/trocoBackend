-- Répare theme_key si Hibernate ddl-auto a échoué (NOT NULL sans DEFAULT).
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS theme_key VARCHAR(40);

UPDATE store_settings
SET theme_key = 'classic'
WHERE theme_key IS NULL;

ALTER TABLE store_settings
    ALTER COLUMN theme_key SET DEFAULT 'classic';

ALTER TABLE store_settings
    ALTER COLUMN theme_key SET NOT NULL;
