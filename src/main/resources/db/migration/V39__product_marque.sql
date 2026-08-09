-- Label marque produit (multi-brands simple, par fournisseur)
ALTER TABLE products ADD COLUMN IF NOT EXISTS marque VARCHAR(120);
