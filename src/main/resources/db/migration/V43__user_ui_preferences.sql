-- Préférences UI par utilisateur (ex. guide première utilisation admin)
ALTER TABLE users ADD COLUMN IF NOT EXISTS ui_preferences_json TEXT;
