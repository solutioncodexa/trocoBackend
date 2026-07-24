-- V13: remove jewelry/gold leftovers, seed packaging catalog

-- Legacy gold entity tables (cours or / types d'or)
DROP TABLE IF EXISTS gold_price_settings CASCADE;
DROP TABLE IF EXISTS gold_types CASCADE;

-- Clear legacy gold columns on existing rows
UPDATE products SET gold_type = NULL WHERE gold_type IS NOT NULL;
UPDATE cart_items SET selected_gold_type = NULL WHERE selected_gold_type IS NOT NULL;
UPDATE order_items SET selected_gold_type = NULL WHERE selected_gold_type IS NOT NULL;

-- Replace jewelry product types with packaging types when jewelry codes are still present
DELETE FROM product_types
WHERE code IN ('BRACELET', 'RING', 'NECKLACE', 'EARRINGS', 'SET');

INSERT INTO product_types (name, code, requires_size, size_options, sort_order)
SELECT v.name, v.code, FALSE, NULL, v.sort_order
FROM (VALUES
    ('Sachet', 'SACHET', 1),
    ('Carton', 'CARTON', 2),
    ('Protection', 'PROTECTION', 3),
    ('Décoration', 'DECORATION', 4),
    ('Matériel', 'MATERIEL', 5)
) AS v(name, code, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM product_types pt WHERE pt.code = v.code
);

-- Replace jewelry collections with packaging collections
DELETE FROM collections
WHERE slug IN ('mariage', 'homme', 'femme');

INSERT INTO collections (name, slug, description, is_active, created_at)
SELECT v.name, v.slug, v.description, TRUE, NOW()
FROM (VALUES
    ('E-commerce', 'e-commerce', 'Emballages pour boutiques en ligne'),
    ('Retail', 'retail', 'Solutions pour points de vente'),
    ('Sur-mesure', 'sur-mesure', 'Créations personnalisées logo & format')
) AS v(name, slug, description)
WHERE NOT EXISTS (
    SELECT 1 FROM collections c WHERE c.slug = v.slug
);
