ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS attributes_json TEXT;
