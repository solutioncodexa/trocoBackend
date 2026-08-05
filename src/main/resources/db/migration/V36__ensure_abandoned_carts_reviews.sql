-- Filet : tables conversion (avis + paniers abandonnés) si V25 non appliquée / schéma partiel

CREATE TABLE IF NOT EXISTS product_reviews (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    author_name     VARCHAR(120) NOT NULL,
    author_email    VARCHAR(255),
    rating          INT NOT NULL,
    title           VARCHAR(200),
    body            TEXT NOT NULL,
    approved        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_reviews_rating_v36 CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product
    ON product_reviews (fournisseur_id, product_id, approved);

CREATE TABLE IF NOT EXISTS abandoned_carts (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    session_key     VARCHAR(80) NOT NULL,
    recovery_token  VARCHAR(64) NOT NULL,
    customer_email  VARCHAR(255),
    customer_phone  VARCHAR(50),
    customer_name   VARCHAR(200),
    cart_json       TEXT NOT NULL,
    cart_total      NUMERIC(12, 2),
    item_count      INT NOT NULL DEFAULT 0,
    reminder_sent   BOOLEAN NOT NULL DEFAULT FALSE,
    recovered       BOOLEAN NOT NULL DEFAULT FALSE,
    remind_at       TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_abandoned_cart_token_v36 UNIQUE (recovery_token),
    CONSTRAINT uq_abandoned_cart_session_v36 UNIQUE (fournisseur_id, session_key)
);

CREATE INDEX IF NOT EXISTS idx_abandoned_carts_remind
    ON abandoned_carts (fournisseur_id, reminder_sent, recovered, remind_at);

ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS meta_pixel_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS tiktok_pixel_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS google_ads_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS google_analytics_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS abandoned_cart_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS abandoned_cart_delay_minutes INT NOT NULL DEFAULT 60,
    ADD COLUMN IF NOT EXISTS whatsapp_order_template TEXT;
