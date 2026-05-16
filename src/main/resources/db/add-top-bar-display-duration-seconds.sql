-- PostgreSQL : ajout durée d'affichage par message (top bar). À exécuter si spring.jpa.hibernate.ddl-auto=validate.
ALTER TABLE top_bar_messages
    ADD COLUMN IF NOT EXISTS display_duration_seconds INTEGER NOT NULL DEFAULT 7;

COMMENT ON COLUMN top_bar_messages.display_duration_seconds IS
    'Secondes d''affichage de ce message avant rotation vers le suivant (si plusieurs messages actifs).';

-- Si des lignes ont encore NULL (colonne ajoutée sans défaut) :
UPDATE top_bar_messages SET display_duration_seconds = 7 WHERE display_duration_seconds IS NULL;
