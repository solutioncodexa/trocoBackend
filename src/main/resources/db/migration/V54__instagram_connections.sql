-- Connexion OAuth Instagram d'une boutique (une par boutique). Le jeton est stocké chiffré (AES-GCM).
CREATE TABLE IF NOT EXISTS instagram_connections (
    id             BIGSERIAL PRIMARY KEY,
    fournisseur_id BIGINT       NOT NULL,
    ig_user_id     VARCHAR(40)  NOT NULL,
    username       VARCHAR(100),
    access_token   TEXT         NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    refreshed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ig_connections_tenant ON instagram_connections (fournisseur_id);
