-- Lot marché marocain : frais par ville, PayZone / virement, campagnes, parrainage, retours.

ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS payment_payzone_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS payment_transfer_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS transfer_instructions TEXT;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS seasonal_code VARCHAR(40);

CREATE TABLE IF NOT EXISTS shipping_city_rates (
    id BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT NOT NULL,
    carrier_code VARCHAR(40) NOT NULL,
    city VARCHAR(80) NOT NULL,
    city_key VARCHAR(80) NOT NULL,
    fee NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_shipping_city_rate UNIQUE (fournisseur_id, carrier_code, city_key)
);

CREATE INDEX IF NOT EXISTS idx_shipping_city_rates_fid ON shipping_city_rates (fournisseur_id);

INSERT INTO shipping_carriers (
    fournisseur_id, code, name, enabled, base_fee, free_above, tracking_url_template,
    eta_days_min, eta_days_max, sort_order, created_at, updated_at
)
SELECT f.id, v.code, v.name, TRUE, v.fee, 500, NULLIF(v.track, ''), v.emin, v.emax, v.ord, NOW(), NOW()
FROM fournisseurs f
CROSS JOIN (VALUES
    ('CHRONODIALI', 'Chronodiali', 30.00, '', 1, 3, 2),
    ('GLOVO', 'Glovo', 25.00, '', 0, 1, 3)
) AS v(code, name, fee, track, emin, emax, ord)
WHERE NOT EXISTS (
    SELECT 1 FROM shipping_carriers c WHERE c.fournisseur_id = f.id AND upper(c.code) = v.code
);

INSERT INTO shipping_city_rates (fournisseur_id, carrier_code, city, city_key, fee, created_at, updated_at)
SELECT c.fournisseur_id, c.code, v.city, v.city_key, c.base_fee + v.extra, NOW(), NOW()
FROM shipping_carriers c
CROSS JOIN (VALUES
    ('Casablanca', 'casablanca', 0),
    ('Rabat', 'rabat', 0),
    ('Marrakech', 'marrakech', 10),
    ('Fès', 'fes', 10),
    ('Tanger', 'tanger', 15),
    ('Agadir', 'agadir', 15)
) AS v(city, city_key, extra)
WHERE NOT EXISTS (
    SELECT 1 FROM shipping_city_rates r
    WHERE r.fournisseur_id = c.fournisseur_id
      AND upper(r.carrier_code) = upper(c.code)
      AND r.city_key = v.city_key
);

CREATE TABLE IF NOT EXISTS seasonal_campaigns (
    id BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    starts_on DATE,
    ends_on DATE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_seasonal_campaign UNIQUE (fournisseur_id, code)
);

INSERT INTO seasonal_campaigns (fournisseur_id, code, title, discount_percent, enabled, created_at, updated_at)
SELECT f.id, v.code, v.title, v.pct, FALSE, NOW(), NOW()
FROM fournisseurs f
CROSS JOIN (VALUES
    ('RAMADAN', 'Ramadan', 10),
    ('AID', 'Aïd', 10),
    ('RENTREE', 'Rentrée', 5)
) AS v(code, title, pct)
WHERE NOT EXISTS (
    SELECT 1 FROM seasonal_campaigns s WHERE s.fournisseur_id = f.id AND s.code = v.code
);

CREATE TABLE IF NOT EXISTS referral_codes (
    id BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    reward_mad NUMERIC(12,2) NOT NULL DEFAULT 0,
    referrer_label VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    uses_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_referral_code UNIQUE (fournisseur_id, code)
);

CREATE TABLE IF NOT EXISTS order_returns (
    id BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    reason TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
    refund_amount NUMERIC(12,2),
    customer_phone VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_returns_fid ON order_returns (fournisseur_id);
