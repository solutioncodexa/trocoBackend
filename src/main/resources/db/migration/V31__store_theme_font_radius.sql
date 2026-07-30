-- Répare font_pair / radius_preset si Hibernate ddl-auto a échoué (NOT NULL sans DEFAULT).
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS font_pair VARCHAR(40);

ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS radius_preset VARCHAR(20);

UPDATE store_settings
SET font_pair = 'display_sans'
WHERE font_pair IS NULL;

UPDATE store_settings
SET radius_preset = 'soft'
WHERE radius_preset IS NULL;

ALTER TABLE store_settings
    ALTER COLUMN font_pair SET DEFAULT 'display_sans';

ALTER TABLE store_settings
    ALTER COLUMN radius_preset SET DEFAULT 'soft';

ALTER TABLE store_settings
    ALTER COLUMN font_pair SET NOT NULL;

ALTER TABLE store_settings
    ALTER COLUMN radius_preset SET NOT NULL;
