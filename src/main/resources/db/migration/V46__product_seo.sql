-- SEO par produit : titre et description meta personnalisables (sinon dérivés du nom / de la description).
ALTER TABLE products ADD COLUMN IF NOT EXISTS seo_title VARCHAR(200);
ALTER TABLE products ADD COLUMN IF NOT EXISTS seo_description VARCHAR(500);
