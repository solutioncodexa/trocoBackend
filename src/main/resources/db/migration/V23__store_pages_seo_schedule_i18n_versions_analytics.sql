-- SEO, planification, i18n, visibilité blocs, versions, analytics

ALTER TABLE store_pages
    ADD COLUMN IF NOT EXISTS seo_title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS seo_description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS og_image_url VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS publish_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS unpublish_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS title_ar VARCHAR(200),
    ADD COLUMN IF NOT EXISTS seo_title_ar VARCHAR(200),
    ADD COLUMN IF NOT EXISTS seo_description_ar VARCHAR(500);

ALTER TABLE store_page_blocks
    ADD COLUMN IF NOT EXISTS visible_mobile BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS visible_desktop BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS config_json_ar TEXT;

CREATE TABLE IF NOT EXISTS store_page_versions (
    id              BIGSERIAL PRIMARY KEY,
    page_id         BIGINT NOT NULL REFERENCES store_pages (id) ON DELETE CASCADE,
    fournisseur_id  BIGINT NOT NULL,
    label           VARCHAR(200) NOT NULL,
    snapshot_json   TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_page_versions_page
    ON store_page_versions (page_id, created_at DESC);

CREATE TABLE IF NOT EXISTS store_page_analytics_events (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    page_id         BIGINT REFERENCES store_pages (id) ON DELETE SET NULL,
    event_type      VARCHAR(40) NOT NULL,
    path            VARCHAR(255),
    meta_json       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_page_analytics_page_day
    ON store_page_analytics_events (fournisseur_id, page_id, event_type, created_at);
