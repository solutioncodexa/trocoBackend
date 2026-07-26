-- Pages vitrine personnalisables (page builder) + blocs / composants

CREATE TABLE IF NOT EXISTS store_pages (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(120) NOT NULL,
    is_home         BOOLEAN NOT NULL DEFAULT FALSE,
    show_in_nav     BOOLEAN NOT NULL DEFAULT TRUE,
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_store_pages_slug_per_tenant UNIQUE (fournisseur_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_store_pages_fournisseur ON store_pages (fournisseur_id);
CREATE INDEX IF NOT EXISTS idx_store_pages_public
    ON store_pages (fournisseur_id, published, show_in_nav);

CREATE TABLE IF NOT EXISTS store_page_blocks (
    id              BIGSERIAL PRIMARY KEY,
    page_id         BIGINT NOT NULL REFERENCES store_pages (id) ON DELETE CASCADE,
    fournisseur_id  BIGINT NOT NULL,
    block_type      VARCHAR(60) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    config_json     TEXT NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_page_blocks_page ON store_page_blocks (page_id, sort_order);
