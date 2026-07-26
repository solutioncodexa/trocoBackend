-- A/B home, ciblage URL promos/bandeaux, leads, blog, état planning

ALTER TABLE store_pages
    ADD COLUMN IF NOT EXISTS ab_variant VARCHAR(1),
    ADD COLUMN IF NOT EXISTS last_live_state BOOLEAN;

ALTER TABLE promo_modals
    ADD COLUMN IF NOT EXISTS target_paths TEXT;

ALTER TABLE top_bar_messages
    ADD COLUMN IF NOT EXISTS target_paths TEXT;

CREATE TABLE IF NOT EXISTS store_leads (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    lead_type       VARCHAR(40) NOT NULL,
    full_name       VARCHAR(200),
    email           VARCHAR(255),
    phone           VARCHAR(50),
    message         TEXT,
    source_path     VARCHAR(255),
    meta_json       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_leads_fournisseur
    ON store_leads (fournisseur_id, created_at DESC);

CREATE TABLE IF NOT EXISTS store_blog_posts (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    title           VARCHAR(250) NOT NULL,
    slug            VARCHAR(160) NOT NULL,
    excerpt         VARCHAR(500),
    content         TEXT NOT NULL,
    cover_url       VARCHAR(1024),
    seo_title       VARCHAR(200),
    seo_description VARCHAR(500),
    lang            VARCHAR(5) NOT NULL DEFAULT 'fr',
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    publish_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_store_blog_slug UNIQUE (fournisseur_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_store_blog_public
    ON store_blog_posts (fournisseur_id, published, publish_at);
