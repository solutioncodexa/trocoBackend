-- Import Instagram : brouillons de produits à relire avant publication (un post = un brouillon).
-- source_key sert à l'anti-doublon : code du post (liens) ou empreinte SHA-256 de la première photo (envois).
CREATE TABLE IF NOT EXISTS instagram_import_drafts (
    id             BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT,
    source_type    VARCHAR(10)  NOT NULL,
    source_url     VARCHAR(500),
    source_key     VARCHAR(80)  NOT NULL,
    caption        TEXT,
    name           VARCHAR(200),
    description    TEXT,
    price          DOUBLE PRECISION,
    stock          INTEGER      NOT NULL DEFAULT 10,
    category_slug  VARCHAR(255),
    images         TEXT,
    status         VARCHAR(12)  NOT NULL DEFAULT 'PENDING',
    issues         VARCHAR(300),
    ai_extracted   BOOLEAN      NOT NULL DEFAULT FALSE,
    product_id     BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ig_drafts_tenant_status ON instagram_import_drafts (fournisseur_id, status);
CREATE INDEX IF NOT EXISTS idx_ig_drafts_tenant_key ON instagram_import_drafts (fournisseur_id, source_key);
