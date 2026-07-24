ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS custom_logo_url VARCHAR(500);
