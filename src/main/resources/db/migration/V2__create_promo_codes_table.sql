-- =============================================
-- V2: Création de la table promo_codes
-- =============================================

CREATE TABLE IF NOT EXISTS promo_codes (
    id                  BIGSERIAL       PRIMARY KEY,
    code                VARCHAR(20)     NOT NULL UNIQUE,
    type                VARCHAR(20)     NOT NULL DEFAULT 'reusable',
    discount_type       VARCHAR(20)     NOT NULL DEFAULT 'percentage',
    discount_value      DOUBLE PRECISION NOT NULL,
    min_order_amount    DOUBLE PRECISION,
    max_uses            INTEGER,
    current_uses        INTEGER         NOT NULL DEFAULT 0,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_promo_code_code ON promo_codes(code);
CREATE INDEX idx_promo_code_active ON promo_codes(is_active);
