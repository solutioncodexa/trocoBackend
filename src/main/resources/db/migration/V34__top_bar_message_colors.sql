-- Couleurs personnalisées par message du bandeau (vide = style thème par défaut).
ALTER TABLE top_bar_messages
    ADD COLUMN IF NOT EXISTS background_color VARCHAR(32),
    ADD COLUMN IF NOT EXISTS text_color VARCHAR(32);

COMMENT ON COLUMN top_bar_messages.background_color IS
    'Couleur de fond du bandeau pour ce message (hex). NULL = dégradé thème.';
COMMENT ON COLUMN top_bar_messages.text_color IS
    'Couleur du texte pour ce message (hex). NULL = texte thème.';
