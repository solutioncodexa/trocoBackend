-- URLs MinIO/CDN absolues peuvent dépasser 1024 caractères.
ALTER TABLE fournisseurs
    ALTER COLUMN logo_url TYPE VARCHAR(2048);
