-- Photo du hero de la page d'accueil (singleton)
CREATE TABLE IF NOT EXISTS home_hero_settings (
    id BIGINT PRIMARY KEY,
    image_url VARCHAR(1024),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO home_hero_settings (id, image_url, updated_at)
VALUES (
    1,
    'https://images.unsplash.com/photo-1616401784845-180882ba9ba8?w=2000&h=1400&fit=crop&q=85',
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
