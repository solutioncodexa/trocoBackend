-- Lancement de la boutique : une boutique créée par inscription publique reste invisible des clients
-- jusqu'à son lancement (le marchand la prévisualise avec une clé d'aperçu). Les boutiques existantes restent en ligne.
ALTER TABLE fournisseurs ADD COLUMN IF NOT EXISTS storefront_live BOOLEAN NOT NULL DEFAULT TRUE;
