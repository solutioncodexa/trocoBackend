-- Compteur d'usage IA par boutique, par jour et par type (ex. IMAGE) : quotas qui survivent au redémarrage.
CREATE TABLE IF NOT EXISTS ai_usage_daily (
    fournisseur_id BIGINT      NOT NULL,
    kind           VARCHAR(30) NOT NULL,
    usage_day      DATE        NOT NULL,
    used           INTEGER     NOT NULL DEFAULT 0,
    PRIMARY KEY (fournisseur_id, kind, usage_day)
);
