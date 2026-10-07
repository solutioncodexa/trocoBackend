-- Personnalisation des emails clients par le marchand (message ajouté + signature).
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS customer_email_note TEXT,
    ADD COLUMN IF NOT EXISTS customer_email_signature TEXT;
