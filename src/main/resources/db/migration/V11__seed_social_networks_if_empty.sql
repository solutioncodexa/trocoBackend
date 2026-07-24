-- Répare le seed si la table a été créée vide (ex. Hibernate ddl-auto avant Flyway)
INSERT INTO social_networks (network_key, label, url, enabled, display_order, updated_at) VALUES
    ('facebook',  'Facebook',  'https://www.facebook.com/profile.php?id=61589818832364', TRUE, 1, CURRENT_TIMESTAMP),
    ('instagram', 'Instagram', 'https://www.instagram.com/troco/', TRUE, 2, CURRENT_TIMESTAMP),
    ('tiktok',    'TikTok',    'https://www.tiktok.com/@troco1', TRUE, 3, CURRENT_TIMESTAMP),
    ('whatsapp',  'WhatsApp',  'https://wa.me/212684490098', TRUE, 4, CURRENT_TIMESTAMP)
ON CONFLICT (network_key) DO NOTHING;
