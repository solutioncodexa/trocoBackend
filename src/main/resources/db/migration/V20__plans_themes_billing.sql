-- =============================================================================
-- Plans additionnels, thème vitrine, paiements abonnement CMI
-- =============================================================================

-- Plans Pro / Business (Basic déjà seedé en V18)
INSERT INTO plans (code, name, description, price_mad, currency, billing_period, max_products, max_staff, custom_domain, active)
VALUES
    (
        'pro',
        'Pro',
        'Pour les boutiques en croissance — plus de produits, équipe élargie, domaine personnalisé',
        299.00,
        'MAD',
        'MONTHLY',
        2000,
        15,
        TRUE,
        TRUE
    ),
    (
        'business',
        'Business',
        'Pour les marques ambitieuses — catalogue illimité, staff illimité, priorité support',
        599.00,
        'MAD',
        'MONTHLY',
        NULL,
        NULL,
        TRUE,
        TRUE
    )
ON CONFLICT (code) DO NOTHING;

UPDATE plans
SET description = 'Idéal pour démarrer — boutique complète, jusqu''à 500 produits',
    name = 'Starter'
WHERE code = 'basic' AND name = 'Basic';

-- Thème vitrine choisi par le vendeur (nullable d'abord → rempli → NOT NULL)
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS theme_key VARCHAR(40);
UPDATE store_settings SET theme_key = 'classic' WHERE theme_key IS NULL;
ALTER TABLE store_settings ALTER COLUMN theme_key SET DEFAULT 'classic';
DO $$
BEGIN
    ALTER TABLE store_settings ALTER COLUMN theme_key SET NOT NULL;
EXCEPTION
    WHEN others THEN NULL;
END $$;

-- Paiements d'abonnement plateforme (CMI)
CREATE TABLE IF NOT EXISTS subscription_payments (
    id                BIGSERIAL PRIMARY KEY,
    fournisseur_id    BIGINT NOT NULL REFERENCES fournisseurs(id),
    plan_id           BIGINT NOT NULL REFERENCES plans(id),
    oid               VARCHAR(64) NOT NULL UNIQUE,
    amount_mad        NUMERIC(12, 2) NOT NULL,
    currency          VARCHAR(8) NOT NULL DEFAULT 'MAD',
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    cmi_response      TEXT,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_fournisseur
    ON subscription_payments (fournisseur_id, created_at DESC);
