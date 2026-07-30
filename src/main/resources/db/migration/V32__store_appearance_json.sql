-- Apparence vitrine (boutons, cards, hero, header, footer) — JSON flexible.
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS appearance_json TEXT;
