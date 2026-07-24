-- =============================================
-- Troco - Données initiales emballage (PostgreSQL)
-- =============================================

-- Catégories principales
INSERT INTO categories (name, description, slug, show_on_hero, hero_sort_order)
VALUES
  ('Sachets & Pochettes', 'Sachets e-commerce, kraft, bulles, ZIP…', 'sachets-pochettes', true, 1),
  ('Carton & Boites', 'Cartons et boîtes d''emballage', 'carton-boites', true, 2),
  ('Protections', 'Calage, rubans, protections', 'protections', true, 3),
  ('Décorations', 'Étiquettes et décorations colis', 'decorations', true, 4),
  ('Matériels', 'Matériel d''emballage professionnel', 'materiels', true, 5)
ON CONFLICT (slug) DO NOTHING;

-- Sous-catégories
INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets ecom', 'Sachets e-commerce', 'sachets-ecom', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets Vergé', 'Sachets kraft / vergé', 'sachets-verge', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets Bulles', 'Sachets à bulles', 'sachets-bulles', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets Cellophane', 'Sachets cellophane', 'sachets-cellophane', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets DoyPack', 'Sachets doypack', 'sachets-doypack', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sachets ZIP', 'Sachets zip', 'sachets-zip', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Sac Non Tissé', 'Sacs non tissés', 'sac-non-tisse', c.id, false
FROM categories c WHERE c.slug = 'sachets-pochettes'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Boîte Valise', 'Boîtes valise', 'boite-valise', c.id, false
FROM categories c WHERE c.slug = 'carton-boites'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Frisure de Calage', 'Frisure de calage', 'frisure-de-calage', c.id, false
FROM categories c WHERE c.slug = 'protections'
ON CONFLICT (slug) DO NOTHING;

INSERT INTO categories (name, description, slug, parent_id, show_on_hero)
SELECT 'Rubans Adhésifs', 'Rubans adhésifs', 'rubans-adhesifs', c.id, false
FROM categories c WHERE c.slug = 'protections'
ON CONFLICT (slug) DO NOTHING;

-- Utilisateur admin (mot de passe: Admin1234) — hash BCrypt
INSERT INTO users (email, password, role, created_at)
VALUES (
  'admin@troco.ma',
  '$2a$10$cY/rGSGdDaKp/DErk.hlP.qBnM9hF/XCDlQqJAFJyL3kV25Y5TyOO',
  'ADMIN',
  NOW()
)
ON CONFLICT (email) DO UPDATE SET password = EXCLUDED.password, role = 'ADMIN';
