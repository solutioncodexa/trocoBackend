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

-- -----------------------------------------------------------------------------
-- V28 : `INSERT INTO shipping_carriers ... ON CONFLICT (fournisseur_id, code)` exige une contrainte
-- unique. Si la table a été créée par Hibernate (donc sans cette contrainte), on la crée avant,
-- uniquement s'il n'y a pas de doublons (sinon on laisse la migration signaler le problème).
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('shipping_carriers') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_indexes
           WHERE schemaname = current_schema()
             AND tablename = 'shipping_carriers'
             AND indexname = 'uq_shipping_carriers_fid_code'
       )
       AND NOT EXISTS (
           SELECT 1 FROM shipping_carriers GROUP BY fournisseur_id, code HAVING COUNT(*) > 1
       )
    THEN
        CREATE UNIQUE INDEX uq_shipping_carriers_fid_code ON shipping_carriers (fournisseur_id, code);
    END IF;
END
$$;

-- -----------------------------------------------------------------------------
-- Cas général du même problème : colonnes NOT NULL sans valeur par défaut dans des tables créées
-- par Hibernate (created_at, updated_at, booléens…). Les migrations SQL qui insèrent sans les
-- renseigner échouent alors (23502). On pose une valeur par défaut neutre : NOW() pour les
-- horodatages, FALSE pour les booléens. Aucun effet sur les colonnes qui ont déjà un défaut.
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name <> 'flyway_schema_history'
          AND is_nullable = 'NO'
          AND column_default IS NULL
          AND is_identity = 'NO'
          AND (data_type LIKE 'timestamp%' OR data_type = 'boolean')
          AND table_name IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema())
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I SET DEFAULT %s',
            c.table_name,
            c.column_name,
            CASE WHEN c.data_type = 'boolean' THEN 'FALSE' ELSE 'NOW()' END
        );
    END LOOP;
END
$$;
