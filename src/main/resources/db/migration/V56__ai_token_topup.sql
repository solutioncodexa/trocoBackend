-- Solde de tokens IA supplémentaires par boutique (recharge accordée par le super-admin), consommé après le budget du plan.
CREATE TABLE IF NOT EXISTS ai_token_topup (
    fournisseur_id BIGINT PRIMARY KEY,
    balance        BIGINT    NOT NULL DEFAULT 0,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
