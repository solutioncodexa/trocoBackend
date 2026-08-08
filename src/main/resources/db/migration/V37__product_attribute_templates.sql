-- Modèles d'attributs de variantes configurables par boutique / catégorie.
-- category_id NULL = modèle par défaut de la boutique (fallback).
-- Colonnes nullable + ON DELETE CASCADE pour compat Hibernate ddl-auto / Neon.

CREATE TABLE IF NOT EXISTS product_attribute_templates (
    id             BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT,
    category_id    BIGINT,
    axes_json      TEXT,
    CONSTRAINT fk_pat_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE CASCADE
);

-- Un seul modèle par (boutique, catégorie). Les modèles boutique (category_id NULL)
-- sont dédoublonnés côté service (PostgreSQL traite les NULL comme distincts).
CREATE UNIQUE INDEX IF NOT EXISTS ux_pat_fournisseur_category
    ON product_attribute_templates (fournisseur_id, category_id);

CREATE INDEX IF NOT EXISTS ix_pat_fournisseur
    ON product_attribute_templates (fournisseur_id);
