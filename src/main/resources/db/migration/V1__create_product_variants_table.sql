-- =============================================
-- V1: Création de la table product_variants
-- =============================================

CREATE TABLE IF NOT EXISTS product_variants (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    label           VARCHAR(100),
    weight          DOUBLE PRECISION NOT NULL,
    price           DOUBLE PRECISION NOT NULL,
    original_price  DOUBLE PRECISION,
    margin_gain     DOUBLE PRECISION DEFAULT 500.0,
    display_order   INTEGER         NOT NULL DEFAULT 0,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_variant_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_variant_product_id ON product_variants(product_id);
CREATE INDEX idx_variant_default ON product_variants(product_id, is_default);
