package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration CMI (Centre Monétique Interbancaire / NestPay 3D Pay Hosting).
 * Secrets via variables d'environnement — jamais commités.
 */
@ConfigurationProperties(prefix = "app.cmi")
public record CmiProperties(
        boolean enabled,
        /** Si true : simule un paiement réussi sans passer par la gateway CMI (dev / démo). */
        boolean testPass,
        String gatewayUrl,
        String clientId,
        String storeKey,
        String storeType,
        String currency,
        String lang,
        String okUrl,
        String failUrl,
        String callbackUrl,
        String hashAlgorithm
) {
    public CmiProperties {
        if (storeType == null || storeType.isBlank()) storeType = "3d_pay_hosting";
        if (currency == null || currency.isBlank()) currency = "504"; // MAD ISO numeric
        if (lang == null || lang.isBlank()) lang = "fr";
        if (hashAlgorithm == null || hashAlgorithm.isBlank()) hashAlgorithm = "ver3";
    }
}
