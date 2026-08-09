-- Colonne bijoux legacy retirée du modèle Product (showWeight).
-- Sans ça, INSERT produit échoue : null value in column "show_weight" violates not-null.

ALTER TABLE products DROP COLUMN IF EXISTS show_weight;

-- style aussi retiré du modèle (déjà nullable depuis V5) — on drop si encore présent.
ALTER TABLE products DROP COLUMN IF EXISTS style;
