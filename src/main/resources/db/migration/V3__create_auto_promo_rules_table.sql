-- =============================================
-- V3: Création de la table auto_promo_rules
-- =============================================

CREATE TABLE IF NOT EXISTS auto_promo_rules (
    id                  BIGSERIAL       PRIMARY KEY,
    min_order_amount    DOUBLE PRECISION NOT NULL,
    discount_type       VARCHAR(20)     NOT NULL DEFAULT 'percentage',
    discount_value      DOUBLE PRECISION NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);
