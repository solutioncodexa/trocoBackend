-- =============================================================================
-- Matjarona multi-tenant : plans, fournisseurs, domaine, Super Admin
-- =============================================================================

-- Plans d'abonnement
CREATE TABLE IF NOT EXISTS plans (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    price_mad       NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(8)  NOT NULL DEFAULT 'MAD',
    billing_period  VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    max_products    INTEGER,
    max_staff       INTEGER,
    custom_domain   BOOLEAN NOT NULL DEFAULT TRUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

INSERT INTO plans (code, name, description, price_mad, currency, billing_period, max_products, max_staff, custom_domain, active)
VALUES (
    'basic',
    'Basic',
    'Plan Basic Matjarona — boutique en ligne complète avec domaine personnalisé',
    150.00,
    'MAD',
    'MONTHLY',
    500,
    5,
    TRUE,
    TRUE
)
ON CONFLICT (code) DO NOTHING;

-- Fournisseurs (tenants)
CREATE TABLE IF NOT EXISTS fournisseurs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255),
    phone           VARCHAR(50),
    logo_url        VARCHAR(1024),
    primary_color   VARCHAR(20),
    secondary_color VARCHAR(20),
    custom_domain   VARCHAR(255),
    domain_verified BOOLEAN NOT NULL DEFAULT FALSE,
    plan_id         BIGINT REFERENCES plans(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    trial_ends_at   TIMESTAMP,
    subscription_ends_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_fournisseurs_custom_domain
    ON fournisseurs (LOWER(custom_domain))
    WHERE custom_domain IS NOT NULL AND custom_domain <> '';

-- Fournisseur démo (données existantes Troco migrées dessus)
INSERT INTO fournisseurs (id, name, slug, email, plan_id, status, primary_color, secondary_color)
SELECT 1, 'Troco', 'troco', 'admin@troco.ma', p.id, 'ACTIVE', '#0F766E', '#134E4A'
FROM plans p WHERE p.code = 'basic'
ON CONFLICT (slug) DO NOTHING;

SELECT setval(pg_get_serial_sequence('fournisseurs', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM fournisseurs), 1));

-- Colonne fournisseur_id sur les tables métier
ALTER TABLE users ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE products ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE custom_orders ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE carts ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE wishlists ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE promo_codes ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE promo_modals ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE auto_promo_rules ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE top_bar_messages ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE social_networks ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE home_hero_settings ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE featured_products ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE stock_settings ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS fournisseur_id BIGINT REFERENCES fournisseurs(id);

-- Backfill données existantes → tenant Troco (id=1)
UPDATE users SET fournisseur_id = 1 WHERE role IN ('ADMIN', 'STAFF', 'CUSTOMER') AND fournisseur_id IS NULL;
UPDATE categories SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE products SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE orders SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE customers SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE custom_orders SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE carts SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE wishlists SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE promo_codes SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE promo_modals SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE auto_promo_rules SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE top_bar_messages SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE social_networks SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE home_hero_settings SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE featured_products SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE stock_settings SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE stock_movements SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE notifications SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;
UPDATE audit_logs SET fournisseur_id = 1 WHERE fournisseur_id IS NULL;

-- Slug catégorie unique par tenant
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_slug_key;
DROP INDEX IF EXISTS categories_slug_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_fournisseur_slug
    ON categories (fournisseur_id, slug);

-- Numéro de commande unique par tenant
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_number_key;
DROP INDEX IF EXISTS orders_order_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_fournisseur_number
    ON orders (fournisseur_id, order_number);

CREATE INDEX IF NOT EXISTS idx_users_fournisseur ON users (fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_products_fournisseur ON products (fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_orders_fournisseur ON orders (fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_categories_fournisseur ON categories (fournisseur_id);

-- Code promo unique par tenant
ALTER TABLE promo_codes DROP CONSTRAINT IF EXISTS promo_codes_code_key;
DROP INDEX IF EXISTS promo_codes_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_promo_codes_fournisseur_code
    ON promo_codes (fournisseur_id, code);

-- Réseau social unique par tenant
ALTER TABLE social_networks DROP CONSTRAINT IF EXISTS social_networks_network_key_key;
DROP INDEX IF EXISTS social_networks_network_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_social_networks_fournisseur_network_key
    ON social_networks (fournisseur_id, network_key);

-- Paramètres boutique dynamiques (composants CMS)
-- store_settings.fournisseur_id UNIQUE : une config boutique par tenant
CREATE TABLE IF NOT EXISTS store_settings (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL UNIQUE REFERENCES fournisseurs(id) ON DELETE CASCADE,
    site_name       VARCHAR(200),
    tagline         TEXT,
    about_text      TEXT,
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    contact_whatsapp VARCHAR(50),
    contact_city    VARCHAR(120),
    free_shipping_threshold NUMERIC(12, 2),
    facebook_url    VARCHAR(512),
    instagram_url   VARCHAR(512),
    tiktok_url      VARCHAR(512),
    favicon_url     VARCHAR(1024),
    hero_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    categories_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sur_mesure_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

INSERT INTO store_settings (fournisseur_id, site_name, tagline, contact_email, free_shipping_threshold)
SELECT 1, 'Troco', 'Solutions d''emballage e-commerce', 'contact@troco.ma', 750
WHERE NOT EXISTS (SELECT 1 FROM store_settings WHERE fournisseur_id = 1);
