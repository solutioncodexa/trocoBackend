package ma.codexa.troco.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Filet de sécurité Neon/dev : colonnes gaps 2026 + grille tarifaire plans.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PlatformGapsSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        bootstrapStoreSettingsAndOrders();
        bootstrapCommerceTables();
        bootstrapPlanMatrix();
    }

    private void bootstrapStoreSettingsAndOrders() {
        try {
            String[] cols = {
                    "default_locale VARCHAR(10) DEFAULT 'fr'",
                    "supported_locales VARCHAR(40) DEFAULT 'fr,ar,en'",
                    "currency VARCHAR(8) DEFAULT 'MAD'",
                    "currency_rates_json TEXT",
                    "payment_cod_enabled BOOLEAN DEFAULT TRUE",
                    "payment_cmi_enabled BOOLEAN DEFAULT FALSE",
                    "payment_bnpl_enabled BOOLEAN DEFAULT FALSE",
                    "bnpl_provider VARCHAR(40) DEFAULT 'manual'",
                    "loyalty_enabled BOOLEAN DEFAULT FALSE",
                    "loyalty_points_per_mad NUMERIC(12,4) DEFAULT 1",
                    "loyalty_mad_per_point NUMERIC(12,4) DEFAULT 0.10",
                    "privacy_policy_url VARCHAR(1024)",
                    "cookie_consent_required BOOLEAN DEFAULT TRUE",
                    "data_retention_days INT DEFAULT 365",
                    "cndp_notice_version VARCHAR(40)",
                    "shipping_default_carrier VARCHAR(40)",
                    // Filet si Hibernate ddl-auto échoue (NOT NULL sans DEFAULT)
                    "font_pair VARCHAR(40) DEFAULT 'display_sans'",
                    "radius_preset VARCHAR(20) DEFAULT 'soft'",
                    "appearance_json TEXT"
            };
            for (String col : cols) {
                jdbc.execute("ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS " + col);
            }
            jdbc.execute("UPDATE store_settings SET font_pair = 'display_sans' WHERE font_pair IS NULL");
            jdbc.execute("UPDATE store_settings SET radius_preset = 'soft' WHERE radius_preset IS NULL");
            try {
                jdbc.execute("ALTER TABLE store_settings ALTER COLUMN font_pair SET DEFAULT 'display_sans'");
                jdbc.execute("ALTER TABLE store_settings ALTER COLUMN radius_preset SET DEFAULT 'soft'");
                jdbc.execute("ALTER TABLE store_settings ALTER COLUMN font_pair SET NOT NULL");
                jdbc.execute("ALTER TABLE store_settings ALTER COLUMN radius_preset SET NOT NULL");
            } catch (Exception ignored) {
                // Neon / concurrent DDL — colonnes déjà correctes
            }
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_fee DOUBLE PRECISION DEFAULT 0");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS carrier_code VARCHAR(40)");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(120)");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_url VARCHAR(1024)");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_earned INT DEFAULT 0");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_redeemed INT DEFAULT 0");
            jdbc.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(40) DEFAULT 'pending'");
        } catch (Exception e) {
            log.warn("bootstrap_store_settings_failed: {}", e.getMessage());
        }
    }

    private void bootstrapCommerceTables() {
        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS shipping_carriers (
                        id BIGSERIAL PRIMARY KEY,
                        fournisseur_id BIGINT NOT NULL,
                        code VARCHAR(40) NOT NULL,
                        name VARCHAR(120) NOT NULL,
                        enabled BOOLEAN NOT NULL DEFAULT TRUE,
                        base_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
                        free_above NUMERIC(12,2),
                        tracking_url_template VARCHAR(1024),
                        eta_days_min INT NOT NULL DEFAULT 2,
                        eta_days_max INT NOT NULL DEFAULT 5,
                        sort_order INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uq_shipping_carriers_fid_code UNIQUE (fournisseur_id, code)
                    )
                    """);
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS store_api_keys (
                        id BIGSERIAL PRIMARY KEY,
                        fournisseur_id BIGINT NOT NULL,
                        name VARCHAR(120) NOT NULL,
                        key_prefix VARCHAR(16) NOT NULL,
                        key_hash VARCHAR(128) NOT NULL,
                        scopes VARCHAR(255) NOT NULL DEFAULT 'products:read,orders:write',
                        enabled BOOLEAN NOT NULL DEFAULT TRUE,
                        last_used_at TIMESTAMPTZ,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        revoked_at TIMESTAMPTZ
                    )
                    """);
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS loyalty_accounts (
                        id BIGSERIAL PRIMARY KEY,
                        fournisseur_id BIGINT NOT NULL,
                        phone VARCHAR(50) NOT NULL,
                        email VARCHAR(255),
                        points_balance INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uq_loyalty_fid_phone UNIQUE (fournisseur_id, phone)
                    )
                    """);
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS payment_audit_logs (
                        id BIGSERIAL PRIMARY KEY,
                        fournisseur_id BIGINT NOT NULL,
                        order_id BIGINT,
                        order_number VARCHAR(80),
                        provider VARCHAR(40) NOT NULL,
                        event_type VARCHAR(80) NOT NULL,
                        amount NUMERIC(12,2),
                        currency VARCHAR(8),
                        status VARCHAR(40),
                        payload_json TEXT,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            try {
                jdbc.execute("""
                        INSERT INTO shipping_carriers (fournisseur_id, code, name, enabled, base_fee, free_above, tracking_url_template, eta_days_min, eta_days_max, sort_order)
                        SELECT f.id, v.code, v.name, TRUE, v.base_fee, v.free_above, v.tpl, v.emin, v.emax, v.so
                        FROM fournisseurs f
                        CROSS JOIN (VALUES
                            ('AMANA', 'Amana (Barid Al-Maghrib)', 35.00, 500.00, 'https://www.poste.ma/customer/tracking?tracking={tracking}', 2, 5, 1),
                            ('CTM', 'CTM Messagerie', 45.00, 600.00, 'https://www.ctm.ma/suivi?code={tracking}', 1, 3, 2),
                            ('DHL', 'DHL Express', 120.00, NULL::numeric, 'https://www.dhl.com/ma-fr/home/tracking.html?tracking-id={tracking}', 1, 2, 3)
                        ) AS v(code, name, base_fee, free_above, tpl, emin, emax, so)
                        ON CONFLICT (fournisseur_id, code) DO NOTHING
                        """);
            } catch (Exception e) {
                log.warn("bootstrap_shipping_seed_skipped: {}", e.getMessage());
            }
            jdbc.execute("""
                    INSERT INTO permissions (code, label, category, description) VALUES
                    ('API_KEYS_MANAGE', 'Gérer les clés API', 'INTEGRATIONS', 'Créer et révoquer les clés API headless'),
                    ('PRIVACY_MANAGE', 'Conformité CNDP', 'COMPLIANCE', 'Exporter / anonymiser les données personnelles clients')
                    ON CONFLICT (code) DO NOTHING
                    """);
        } catch (Exception e) {
            log.warn("bootstrap_commerce_tables_failed: {}", e.getMessage());
        }
    }

    private void bootstrapPlanMatrix() {
        try {
            jdbc.execute("ALTER TABLE plans ADD COLUMN IF NOT EXISTS max_orders_per_month INT");
            jdbc.execute("ALTER TABLE plans ADD COLUMN IF NOT EXISTS max_pixels INT");
            jdbc.execute("ALTER TABLE plans ADD COLUMN IF NOT EXISTS storage_mb INT");
            jdbc.execute("ALTER TABLE plans ADD COLUMN IF NOT EXISTS features_json TEXT");
            // Uniquement si features_json absent — ne jamais écraser la config Super Admin
            int basic = jdbc.update("""
                    UPDATE plans SET name=COALESCE(NULLIF(name,''),'Basic'), price_mad=COALESCE(price_mad,79),
                    max_products=COALESCE(max_products,50), max_staff=COALESCE(max_staff,1), custom_domain=FALSE,
                    max_orders_per_month=COALESCE(max_orders_per_month,100), max_pixels=COALESCE(max_pixels,1),
                    storage_mb=COALESCE(storage_mb,1024),
                    features_json='{"themes":"basic","pageBuilder":"simple","abTesting":false,"abandonedCart":false,"abandonedCartAdvanced":false,"whatsappBusiness":false,"whatsappMultiTemplates":false,"webhooks":"none","blogSeo":"basic","support":"email","apiHeadless":false,"loyalty":false,"multiCurrency":false}'
                    WHERE code='basic' AND features_json IS NULL
                    """);
            int pro = jdbc.update("""
                    UPDATE plans SET name=COALESCE(NULLIF(name,''),'Pro'), price_mad=COALESCE(price_mad,199),
                    max_products=COALESCE(max_products,500), max_staff=COALESCE(max_staff,3), custom_domain=TRUE,
                    max_orders_per_month=COALESCE(max_orders_per_month,1000), max_pixels=COALESCE(max_pixels,3),
                    storage_mb=COALESCE(storage_mb,10240),
                    features_json='{"themes":"all","pageBuilder":"full","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":false,"whatsappBusiness":true,"whatsappMultiTemplates":false,"webhooks":"order_created","blogSeo":"full","support":"email_chat","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
                    WHERE code='pro' AND features_json IS NULL
                    """);
            int business = jdbc.update("""
                    UPDATE plans SET name=COALESCE(NULLIF(name,''),'Business'), price_mad=COALESCE(price_mad,399),
                    max_staff=COALESCE(max_staff,10), custom_domain=TRUE, storage_mb=COALESCE(storage_mb,51200),
                    features_json='{"themes":"all_early","pageBuilder":"full_versions","abTesting":true,"abandonedCart":true,"abandonedCartAdvanced":true,"whatsappBusiness":true,"whatsappMultiTemplates":true,"webhooks":"all","blogSeo":"full_priority","support":"priority","apiHeadless":true,"loyalty":true,"multiCurrency":true}'
                    WHERE code='business' AND features_json IS NULL
                    """);
            log.info("plan_matrix_bootstrap_ok (null features only) basic={} pro={} business={}", basic, pro, business);
        } catch (Exception e) {
            log.warn("bootstrap_plan_matrix_failed: {}", e.getMessage());
        }
    }
}
