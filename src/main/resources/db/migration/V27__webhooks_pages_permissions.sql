-- Webhooks sortants + permissions pages / webhooks

CREATE TABLE IF NOT EXISTS store_webhooks (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    name            VARCHAR(120) NOT NULL,
    target_url      VARCHAR(1024) NOT NULL,
    secret          VARCHAR(120),
    events          VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_webhooks_fid
    ON store_webhooks (fournisseur_id, enabled);

CREATE TABLE IF NOT EXISTS store_webhook_deliveries (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    webhook_id      BIGINT NOT NULL,
    event_type      VARCHAR(80) NOT NULL,
    status_code     INT,
    success         BOOLEAN NOT NULL DEFAULT FALSE,
    error_message   VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_webhook_deliveries_fid
    ON store_webhook_deliveries (fournisseur_id, created_at DESC);

INSERT INTO permissions (code, label, category, description) VALUES
('PAGES_EDIT', 'Éditer les pages', 'PAGES', 'Créer et modifier pages / composants (sans publier)'),
('PAGES_PUBLISH', 'Publier les pages', 'PAGES', 'Publier, dépublier et planifier les pages'),
('WEBHOOKS_MANAGE', 'Gérer les webhooks', 'INTEGRATIONS', 'Configurer les webhooks Zapier / n8n')
ON CONFLICT (code) DO NOTHING;
