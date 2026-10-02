-- =============================================================================
-- Callback Flyway exécuté AVANT chaque migration (ne modifie aucun checksum de V*).
--
-- Cas visé : un schéma créé auparavant par Hibernate (ddl-auto=update) puis repris par Flyway
-- (baseline-on-migrate). La table `fournisseurs` existe alors déjà avec `domain_verified`
-- NOT NULL et SANS valeur par défaut ; le `CREATE TABLE IF NOT EXISTS` de V18 est ignoré et son
-- INSERT du fournisseur démo échoue (SQL State 23502). On pose le DEFAULT manquant avant V18.
-- Sans effet si la table ou la colonne n'existe pas encore (base vierge).
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'fournisseurs'
          AND column_name = 'domain_verified'
    ) THEN
        ALTER TABLE fournisseurs ALTER COLUMN domain_verified SET DEFAULT FALSE;
    END IF;
END
$$;
