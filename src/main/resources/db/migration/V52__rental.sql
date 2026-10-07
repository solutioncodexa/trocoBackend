-- Location : produits louables (unité configurable, jour par défaut) et réservations sur les lignes de commande.
-- Le stock n'est PAS décrémenté définitivement : la disponibilité d'un jour = stock − réservations actives qui couvrent ce jour.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS rental_enabled   BOOLEAN          NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS rental_unit      VARCHAR(10)      NOT NULL DEFAULT 'DAY',
    ADD COLUMN IF NOT EXISTS rental_deposit   DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS rental_min_units INTEGER          NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS rental_max_units INTEGER;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS rental_start   DATE,
    ADD COLUMN IF NOT EXISTS rental_end     DATE,
    ADD COLUMN IF NOT EXISTS rental_units   INTEGER,
    ADD COLUMN IF NOT EXISTS rental_deposit DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_order_items_rental
    ON order_items (product_id, rental_start, rental_end)
    WHERE rental_start IS NOT NULL;
