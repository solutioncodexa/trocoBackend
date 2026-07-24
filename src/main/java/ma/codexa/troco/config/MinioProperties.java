package ma.codexa.troco.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration MinIO / S3.
 *
 * <p>Le {@code endpoint} est utilisé par le serveur pour les opérations
 * (upload, delete) — typiquement l'URL interne docker {@code http://minio:9000}.
 * Le {@code publicEndpoint} est l'URL publique injectée dans les liens
 * retournés au front (ex. {@code https://files.troco.ma}).</p>
 */
@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        boolean autoCreateBucket,
        boolean publicRead
) {
    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) endpoint = "http://minio:9000";
        if (bucket == null || bucket.isBlank()) bucket = "troco-uploads";
        if (region == null || region.isBlank()) region = "us-east-1";
    }

    /**
     * Endpoint public effectif : tombe sur l'endpoint interne si non configuré
     * (utile en dev où le navigateur tape directement le port 9000).
     */
    public String effectivePublicEndpoint() {
        return (publicEndpoint != null && !publicEndpoint.isBlank()) ? publicEndpoint : endpoint;
    }
}
