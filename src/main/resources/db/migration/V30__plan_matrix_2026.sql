-- Grille tarifaire Matjarona 2026 : Basic 79 / Pro 199 / Business 399

ALTER TABLE plans ADD COLUMN IF NOT EXISTS max_orders_per_month INT;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS max_pixels INT;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS storage_mb INT;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS features_json TEXT;

UPDATE plans SET
    name = 'Basic',
    description = 'Idéal pour démarrer — jusqu''à 50 produits, sous-domaine Matjarona',
    price_mad = 79.00,
    max_products = 50,
    max_staff = 1,
    custom_domain = FALSE,
    max_orders_per_month = 100,
    max_pixels = 1,
    storage_mb = 1024,
    features_json = '{"themes":"basic","pageBuilder":"simple","abTesting":false,"abandonedCart":false,"abandonedCartAdvanced":false,"whatsappBusiness":false,"whatsappMultiTemplates":false,"webhooks":"none","blogSeo":"basic","support":"email","apiHeadless":false,"loyalty":false,"multiCurrency":false}'
WHERE code = 'basic';

UPDATE plans SET
    name = 'Pro',
    description = 'Pour croître — 500 produits, domaine personnalisé, WhatsApp, panier abandonné',
    price_mad = 199.00,
    max_products = 500,
    max_staff = 3,
    custom_domain = TRUE,
    max_orders_per_month = 1000,
    max_pixels = 3,
    storage_mb = 10240,
    features_json = '{"themes":"all","pageBuilder":"full","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":false,"whatsappBusiness":true,"whatsappMultiTemplates":false,"webhooks":"order_created","blogSeo":"full","support":"email_chat","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
WHERE code = 'pro';

UPDATE plans SET
    name = 'Business',
    description = 'Pour scaler — illimité, A/B avancé, webhooks complets, onboarding dédié',
    price_mad = 399.00,
    max_products = NULL,
    max_staff = 10,
    custom_domain = TRUE,
    max_orders_per_month = NULL,
    max_pixels = NULL,
    storage_mb = 51200,
    features_json = '{"themes":"all_early","pageBuilder":"full_versions","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":true,"whatsappBusiness":true,"whatsappMultiTemplates":true,"webhooks":"all","blogSeo":"full_priority","support":"priority","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
WHERE code = 'business';

-- Garantir existence si seed manquant
INSERT INTO plans (code, name, description, price_mad, currency, billing_period, max_products, max_staff, custom_domain, active, max_orders_per_month, max_pixels, storage_mb, features_json)
VALUES
(
    'basic', 'Basic',
    'Idéal pour démarrer — jusqu''à 50 produits, sous-domaine Matjarona',
    79.00, 'MAD', 'MONTHLY', 50, 1, FALSE, TRUE, 100, 1, 1024,
    '{"themes":"basic","pageBuilder":"simple","abTesting":false,"abandonedCart":false,"abandonedCartAdvanced":false,"whatsappBusiness":false,"whatsappMultiTemplates":false,"webhooks":"none","blogSeo":"basic","support":"email","apiHeadless":false,"loyalty":false,"multiCurrency":false}'
),
(
    'pro', 'Pro',
    'Pour croître — 500 produits, domaine personnalisé, WhatsApp, panier abandonné',
    199.00, 'MAD', 'MONTHLY', 500, 3, TRUE, TRUE, 1000, 3, 10240,
    '{"themes":"all","pageBuilder":"full","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":false,"whatsappBusiness":true,"whatsappMultiTemplates":false,"webhooks":"order_created","blogSeo":"full","support":"email_chat","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
),
(
    'business', 'Business',
    'Pour scaler — illimité, A/B avancé, webhooks complets, onboarding dédié',
    399.00, 'MAD', 'MONTHLY', NULL, 10, TRUE, TRUE, NULL, NULL, 51200,
    '{"themes":"all_early","pageBuilder":"full_versions","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":true,"whatsappBusiness":true,"whatsappMultiTemplates":true,"webhooks":"all","blogSeo":"full_priority","support":"priority","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
)
ON CONFLICT (code) DO NOTHING;
