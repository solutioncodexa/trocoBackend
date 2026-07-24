-- Plusieurs images pour le hero (JSON array), migration depuis image_url
ALTER TABLE home_hero_settings
    ADD COLUMN IF NOT EXISTS image_urls TEXT;

UPDATE home_hero_settings
SET image_urls = CASE
    WHEN image_url IS NOT NULL AND btrim(image_url) <> '' THEN
        '["' || replace(image_url, '"', '\"') || '"]'
    ELSE
        '["https://images.unsplash.com/photo-1616401784845-180882ba9ba8?w=2000&h=1400&fit=crop&q=85"]'
END
WHERE image_urls IS NULL OR btrim(image_urls) = '';
