-- V14: team members, permissions catalog, audit trail

ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE users SET active = TRUE WHERE active IS NULL;
UPDATE users SET full_name = COALESCE(full_name, split_part(email, '@', 1)) WHERE full_name IS NULL;

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    label VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_code VARCHAR(80) NOT NULL,
    PRIMARY KEY (user_id, permission_code)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    user_full_name VARCHAR(255),
    action VARCHAR(80) NOT NULL,
    entity_name VARCHAR(80),
    entity_id VARCHAR(80),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created ON audit_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_created ON audit_logs (entity_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created ON audit_logs (action, created_at DESC);

INSERT INTO permissions (code, label, category, description) VALUES
('PRODUCTS_VIEW', 'Voir les produits', 'PRODUITS', 'Consulter le catalogue produits'),
('PRODUCTS_CREATE', 'Créer des produits', 'PRODUITS', 'Ajouter de nouveaux produits'),
('PRODUCTS_UPDATE', 'Modifier des produits', 'PRODUITS', 'Modifier les produits existants'),
('PRODUCTS_DELETE', 'Supprimer des produits', 'PRODUITS', 'Supprimer des produits'),
('ORDERS_VIEW', 'Voir les commandes', 'COMMANDES', 'Consulter les commandes site'),
('ORDERS_UPDATE', 'Gérer les commandes', 'COMMANDES', 'Changer le statut / supprimer des commandes'),
('STOCK_VIEW', 'Voir le stock', 'STOCK', 'Consulter stock et historique'),
('STOCK_ADJUST', 'Ajuster le stock', 'STOCK', 'Achat, vente directe, inventaire'),
('CUSTOM_ORDERS_VIEW', 'Voir devis / sur-mesure', 'DEVIS', 'Consulter les demandes'),
('CUSTOM_ORDERS_UPDATE', 'Gérer devis / sur-mesure', 'DEVIS', 'Mettre à jour les demandes'),
('CATALOG_MANAGE', 'Gérer le catalogue', 'CATALOGUE', 'Catégories, types, collections, sélectionnés, hero'),
('CONTENT_MANAGE', 'Gérer le contenu', 'CONTENU', 'Top-bar, promos, réseaux, codes promo'),
('STATS_VIEW', 'Voir les statistiques', 'STATS', 'Tableau de bord et revenus'),
('MEMBERS_MANAGE', 'Gérer les membres', 'MEMBRES', 'Créer et administrer les comptes équipe'),
('AUDIT_VIEW', 'Voir l''audit', 'AUDIT', 'Consulter l''historique des actions')
ON CONFLICT (code) DO NOTHING;
