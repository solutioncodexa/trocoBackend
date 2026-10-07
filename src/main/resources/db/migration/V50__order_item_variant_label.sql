-- Libellé lisible de la variante commandée (ex. « Quantité : 10 ») : affiché au vendeur, dans les emails et WhatsApp.
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_label VARCHAR(255);
