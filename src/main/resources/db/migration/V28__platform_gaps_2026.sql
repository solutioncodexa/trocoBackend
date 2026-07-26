-- Gaps B2B/B2C 2026 (colonnes nullable + DEFAULT pour compat Hibernate ddl-auto / Neon)

ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS default_locale VARCHAR(10) DEFAULT 'fr';
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS supported_locales VARCHAR(40) DEFAULT 'fr,ar,en';
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS currency VARCHAR(8) DEFAULT 'MAD';
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS currency_rates_json TEXT;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS payment_cod_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS payment_cmi_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS payment_bnpl_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS bnpl_provider VARCHAR(40) DEFAULT 'manual';
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS loyalty_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS loyalty_points_per_mad NUMERIC(12, 4) DEFAULT 1;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS loyalty_mad_per_point NUMERIC(12, 4) DEFAULT 0.10;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS privacy_policy_url VARCHAR(1024);
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS cookie_consent_required BOOLEAN DEFAULT TRUE;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS data_retention_days INT DEFAULT 365;
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS cndp_notice_version VARCHAR(40);
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS shipping_default_carrier VARCHAR(40);

UPDATE store_settings SET default_locale = COALESCE(default_locale, 'fr');
UPDATE store_settings SET supported_locales = COALESCE(supported_locales, 'fr,ar,en');
UPDATE store_settings SET currency = COALESCE(currency, 'MAD');
UPDATE store_settings SET currency_rates_json = COALESCE(currency_rates_json, '{"EUR":0.091,"USD":0.100,"GBP":0.078}');
UPDATE store_settings SET payment_cod_enabled = COALESCE(payment_cod_enabled, TRUE);
UPDATE store_settings SET payment_cmi_enabled = COALESCE(payment_cmi_enabled, FALSE);
UPDATE store_settings SET payment_bnpl_enabled = COALESCE(payment_bnpl_enabled, FALSE);
UPDATE store_settings SET bnpl_provider = COALESCE(bnpl_provider, 'manual');
UPDATE store_settings SET loyalty_enabled = COALESCE(loyalty_enabled, FALSE);
UPDATE store_settings SET loyalty_points_per_mad = COALESCE(loyalty_points_per_mad, 1);
UPDATE store_settings SET loyalty_mad_per_point = COALESCE(loyalty_mad_per_point, 0.10);
UPDATE store_settings SET cookie_consent_required = COALESCE(cookie_consent_required, TRUE);
UPDATE store_settings SET data_retention_days = COALESCE(data_retention_days, 365);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_fee DOUBLE PRECISION DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS carrier_code VARCHAR(40);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(120);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_url VARCHAR(1024);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_earned INT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_redeemed INT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(40) DEFAULT 'pending';

UPDATE orders SET shipping_fee = COALESCE(shipping_fee, 0);
UPDATE orders SET loyalty_points_earned = COALESCE(loyalty_points_earned, 0);
UPDATE orders SET loyalty_points_redeemed = COALESCE(loyalty_points_redeemed, 0);
UPDATE orders SET payment_status = COALESCE(payment_status, 'pending');

CREATE TABLE IF NOT EXISTS shipping_carriers (
    id                   BIGSERIAL PRIMARY KEY,
    fournisseur_id       BIGINT NOT NULL,
    code                 VARCHAR(40) NOT NULL,
    name                 VARCHAR(120) NOT NULL,
    enabled              BOOLEAN NOT NULL DEFAULT TRUE,
    base_fee             NUMERIC(12, 2) NOT NULL DEFAULT 0,
    free_above           NUMERIC(12, 2),
    tracking_url_template VARCHAR(1024),
    eta_days_min         INT NOT NULL DEFAULT 2,
    eta_days_max         INT NOT NULL DEFAULT 5,
    sort_order           INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_shipping_carriers_fid_code UNIQUE (fournisseur_id, code)
);

CREATE INDEX IF NOT EXISTS idx_shipping_carriers_fid
    ON shipping_carriers (fournisseur_id, enabled);

CREATE TABLE IF NOT EXISTS store_api_keys (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    name            VARCHAR(120) NOT NULL,
    key_prefix      VARCHAR(16) NOT NULL,
    key_hash        VARCHAR(128) NOT NULL,
    scopes          VARCHAR(255) NOT NULL DEFAULT 'products:read,orders:write',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_store_api_keys_fid
    ON store_api_keys (fournisseur_id, enabled);
CREATE INDEX IF NOT EXISTS idx_store_api_keys_prefix
    ON store_api_keys (key_prefix);

CREATE TABLE IF NOT EXISTS loyalty_accounts (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    phone           VARCHAR(50) NOT NULL,
    email           VARCHAR(255),
    points_balance  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_loyalty_fid_phone UNIQUE (fournisseur_id, phone)
);

CREATE INDEX IF NOT EXISTS idx_loyalty_accounts_fid
    ON loyalty_accounts (fournisseur_id);

CREATE TABLE IF NOT EXISTS payment_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    order_id        BIGINT,
    order_number    VARCHAR(80),
    provider        VARCHAR(40) NOT NULL,
    event_type      VARCHAR(80) NOT NULL,
    amount          NUMERIC(12, 2),
    currency        VARCHAR(8),
    status          VARCHAR(40),
    payload_json    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_audit_fid
    ON payment_audit_logs (fournisseur_id, created_at DESC);

INSERT INTO shipping_carriers (fournisseur_id, code, name, enabled, base_fee, free_above, tracking_url_template, eta_days_min, eta_days_max, sort_order)
SELECT f.id, v.code, v.name, TRUE, v.base_fee, v.free_above, v.tracking_url_template, v.eta_min, v.eta_max, v.sort_order
FROM fournisseurs f
CROSS JOIN (VALUES
    ('AMANA', 'Amana (Barid Al-Maghrib)', 35.00, 500.00, 'https://www.poste.ma/customer/tracking?tracking={tracking}', 2, 5, 1),
    ('CTM', 'CTM Messagerie', 45.00, 600.00, 'https://www.ctm.ma/suivi?code={tracking}', 1, 3, 2),
    ('DHL', 'DHL Express', 120.00, NULL::numeric, 'https://www.dhl.com/ma-fr/home/tracking.html?tracking-id={tracking}', 1, 2, 3)
) AS v(code, name, base_fee, free_above, tracking_url_template, eta_min, eta_max, sort_order)
ON CONFLICT (fournisseur_id, code) DO NOTHING;

INSERT INTO permissions (code, label, category, description) VALUES
('API_KEYS_MANAGE', 'Gérer les clés API', 'INTEGRATIONS', 'Créer et révoquer les clés API headless'),
('PRIVACY_MANAGE', 'Conformité CNDP', 'COMPLIANCE', 'Exporter / anonymiser les données personnelles clients')
ON CONFLICT (code) DO NOTHING;
