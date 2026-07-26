-- Sitemap/preview pages + sections globales (mega-menu, footer, sticky CTA)

ALTER TABLE store_pages
    ADD COLUMN IF NOT EXISTS preview_token VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_store_pages_preview_token
    ON store_pages (preview_token)
    WHERE preview_token IS NOT NULL;

CREATE TABLE IF NOT EXISTS store_global_sections (
    id              BIGSERIAL PRIMARY KEY,
    fournisseur_id  BIGINT NOT NULL,
    section_key     VARCHAR(40) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    config_json     TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_store_global_section UNIQUE (fournisseur_id, section_key)
);

CREATE INDEX IF NOT EXISTS idx_store_global_sections_fid
    ON store_global_sections (fournisseur_id);
