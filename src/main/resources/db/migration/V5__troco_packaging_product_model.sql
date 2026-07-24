-- Troco packaging model: hierarchical categories + attribute variants + Woo import ids

ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS external_woo_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_parent'
  ) THEN
    ALTER TABLE categories
      ADD CONSTRAINT fk_categories_parent
      FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL;
  END IF;
END $$;

ALTER TABLE products ADD COLUMN IF NOT EXISTS short_description TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS sku VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS external_woo_id BIGINT;

ALTER TABLE products ALTER COLUMN weight DROP NOT NULL;
ALTER TABLE products ALTER COLUMN gold_type DROP NOT NULL;
ALTER TABLE products ALTER COLUMN product_type DROP NOT NULL;
ALTER TABLE products ALTER COLUMN style DROP NOT NULL;

ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS attribute_name VARCHAR(100);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS attribute_value VARCHAR(150);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS stock INTEGER DEFAULT 0;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS sku VARCHAR(100);

ALTER TABLE product_variants ALTER COLUMN weight DROP NOT NULL;

UPDATE product_variants SET stock = 0 WHERE stock IS NULL;
