-- =============================================
-- GoldYara - Données initiales (PostgreSQL)
-- Exécuter après le premier démarrage (tables créées par Hibernate)
-- ou adapter si vous créez les tables manuellement.
-- =============================================

-- Catégories (slug unique)
INSERT INTO categories (name, description, slug)
VALUES
  ('Beldi', 'Bijoux traditionnels marocains', 'beldi'),
  ('Moderne', 'Designs contemporains', 'modern')
ON CONFLICT (slug) DO NOTHING;

-- Collections (slug unique)
INSERT INTO collections (name, slug, description, is_active, created_at)
VALUES
  ('Mariage', 'mariage', 'Collections pour les mariages et fiançailles', true, NOW()),
  ('Homme', 'homme', 'Collections masculines', true, NOW()),
  ('Femme', 'femme', 'Collections féminines', true, NOW())
ON CONFLICT (slug) DO NOTHING;

-- Types de produits (code unique)
INSERT INTO product_types (name, code, requires_size, size_options, sort_order)
VALUES
  ('Bracelet', 'BRACELET', true, '16cm,17cm,18cm,19cm,20cm,21cm', 1),
  ('Bague', 'RING', true, '48,50,52,54,56,58,60,62,64,66', 2),
  ('Collier', 'NECKLACE', true, '40cm,42cm,45cm,50cm,55cm,60cm', 3),
  ('Boucles d''oreilles', 'EARRINGS', false, NULL, 4),
  ('Parure', 'SET', false, NULL, 5)
ON CONFLICT (code) DO NOTHING;

-- Types d'or (code unique)
INSERT INTO gold_types (name, code, sort_order)
VALUES
  ('Or Jaune', 'YELLOW', 1),
  ('Or Blanc', 'WHITE', 2),
  ('Or Rose', 'ROSE', 3)
ON CONFLICT (code) DO NOTHING;

-- Paramètres : prix au gramme uniquement. Chaque produit a sa propre marge (margin_gain).
INSERT INTO gold_price_settings (price_per_gram)
SELECT 650
WHERE NOT EXISTS (SELECT 1 FROM gold_price_settings LIMIT 1);

-- Utilisateur admin (mot de passe: Admin1234)
-- Hash BCrypt pour "Admin1234" (généré avec force 10)
INSERT INTO users (email, password, role, created_at)
VALUES (
  'admin@goldyara.ma',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'ADMIN',
  NOW()
)
ON CONFLICT DO NOTHING;

-- Exemple de produits (à exécuter après les catégories et product_types)
-- Remplacez category_id par les IDs réels de votre table categories (1=Beldi, 2=Moderne)
INSERT INTO products (
  name, description, price, original_price, weight, stock,
  collection, available_sizes, category_id, product_type, gold_type, style, badges, created_at, updated_at
)
SELECT
  'Bracelet Beldi Traditionnel',
  'Un magnifique bracelet beldi en or 18 carats, travaillé à la main avec des motifs traditionnels marocains.',
  10500, 12500, 25, 5,
  'mariage', '16cm,17cm,18cm,19cm,20cm,21cm',
  c.id, 'BRACELET', 'yellow', 'BELDI', 'bestseller,promo',
  NOW(), NOW()
FROM categories c WHERE c.slug = 'beldi' LIMIT 1;

INSERT INTO products (
  name, description, price, original_price, weight, stock,
  collection, available_sizes, category_id, product_type, gold_type, style, badges, created_at, updated_at
)
SELECT
  'Collier Moderne Élégance',
  'Collier moderne en or jaune 18 carats avec un design épuré et contemporain.',
  18900, NULL, 35, 3,
  NULL, '40cm,42cm,45cm,50cm,55cm,60cm',
  c.id, 'NECKLACE', 'yellow', 'MODERNE', 'new',
  NOW(), NOW()
FROM categories c WHERE c.slug = 'modern' LIMIT 1;

INSERT INTO products (
  name, description, price, original_price, weight, stock,
  collection, available_sizes, category_id, product_type, gold_type, style, badges, created_at, updated_at
)
SELECT
  'Bague Diamant Solitaire',
  'Bague solitaire en or blanc 18 carats sertie d''un diamant brillant.',
  35000, NULL, 8, 2,
  'mariage', '48,50,52,54,56,58,60,62,64,66',
  c.id, 'RING', 'white', 'MODERNE', 'new,bestseller',
  NOW(), NOW()
FROM categories c WHERE c.slug = 'modern' LIMIT 1;

INSERT INTO products (
  name, description, price, original_price, weight, stock,
  collection, available_sizes, category_id, product_type, gold_type, style, badges, created_at, updated_at
)
SELECT
  'Boucles d''oreilles Beldi',
  'Boucles d''oreilles pendantes de style beldi, ornées de motifs filigranés traditionnels.',
  8500, NULL, 12, 8,
  NULL, NULL,
  c.id, 'EARRINGS', 'yellow', 'BELDI', NULL,
  NOW(), NOW()
FROM categories c WHERE c.slug = 'beldi' LIMIT 1;

INSERT INTO products (
  name, description, price, original_price, weight, stock,
  collection, available_sizes, category_id, product_type, gold_type, style, badges, created_at, updated_at
)
SELECT
  'Parure Complète Royale',
  'Ensemble complet collier, bracelet, boucles d''oreilles et bague. Style beldi royal.',
  65000, 75000, 120, 1,
  NULL, NULL,
  c.id, 'SET', 'yellow', 'BELDI', 'bestseller,promo',
  NOW(), NOW()
FROM categories c WHERE c.slug = 'beldi' LIMIT 1;

-- Images pour le premier produit (optionnel, adapter product_id si besoin)
-- INSERT INTO images (url, alt, is_primary, display_order, product_id)
-- SELECT 'https://images.unsplash.com/photo-1611652022419-a9419f74343d?w=800', 'Bracelet Beldi', true, 1, id FROM products WHERE name = 'Bracelet Beldi Traditionnel' LIMIT 1;
