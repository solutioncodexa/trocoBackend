-- Essai gratuit : marqueur d'envoi du rappel « fin d'essai proche ».
ALTER TABLE fournisseurs ADD COLUMN IF NOT EXISTS trial_reminder_sent_at TIMESTAMP;
