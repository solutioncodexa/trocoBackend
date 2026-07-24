-- Stock settings (singleton) + variant stock fields + stock movements

CREATE TABLE IF NOT EXISTS stock_settings (
    id                  BIGSERIAL PRIMARY KEY,
    default_safety_stock INT NOT NULL DEFAULT 10,
    expiry_alert_days   INT NOT NULL DEFAULT 30,
    alerts_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    low_stock_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expiry_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO stock_settings (id, default_safety_stock, expiry_alert_days, alerts_enabled, low_stock_alerts_enabled, expiry_alerts_enabled)
SELECT 1, 10, 30, TRUE, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM stock_settings WHERE id = 1);

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS safety_stock INT NULL,
    ADD COLUMN IF NOT EXISTS reorder_qty INT NULL,
    ADD COLUMN IF NOT EXISTS expiry_date DATE NULL,
    ADD COLUMN IF NOT EXISTS last_restocked_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS stock_movements (
    id            BIGSERIAL PRIMARY KEY,
    variant_id    BIGINT NULL REFERENCES product_variants(id) ON DELETE SET NULL,
    product_id    BIGINT NULL REFERENCES products(id) ON DELETE SET NULL,
    type          VARCHAR(20) NOT NULL,
    quantity      INT NOT NULL,
    stock_before  INT NOT NULL,
    stock_after   INT NOT NULL,
    reason        VARCHAR(500),
    order_id      BIGINT NULL,
    created_by    VARCHAR(255),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_variant ON stock_movements(variant_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_order ON stock_movements(order_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created ON stock_movements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_variants_expiry ON product_variants(expiry_date);
