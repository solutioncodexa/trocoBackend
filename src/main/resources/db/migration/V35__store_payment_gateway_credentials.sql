-- Clés paiement par boutique (Stripe / PayPal / CMI) + horodatage de vérification
ALTER TABLE store_settings
    ADD COLUMN IF NOT EXISTS payment_stripe_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stripe_publishable_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_secret_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_verified_at TIMESTAMP,

    ADD COLUMN IF NOT EXISTS payment_paypal_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS paypal_client_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS paypal_client_secret VARCHAR(255),
    ADD COLUMN IF NOT EXISTS paypal_mode VARCHAR(20) DEFAULT 'sandbox',
    ADD COLUMN IF NOT EXISTS paypal_verified_at TIMESTAMP,

    ADD COLUMN IF NOT EXISTS cmi_client_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cmi_store_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cmi_verified_at TIMESTAMP;
