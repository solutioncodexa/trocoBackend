-- Snapshots look par thème (classic/minimal/bold/elegant).
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS theme_presets_json TEXT;
