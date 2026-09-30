-- Vérification d'email des comptes admin boutique (inscription publique).
-- DEFAULT TRUE : les comptes existants sont considérés comme vérifiés.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(80);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_email_verification_token ON users (email_verification_token);
