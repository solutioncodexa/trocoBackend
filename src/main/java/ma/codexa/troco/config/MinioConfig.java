package ma.codexa.troco.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration MinIO/S3.
 *
 * <p>Active automatiquement un client MinIO lorsque {@code app.storage.type=minio}.
 * Au démarrage de l'application : crée le bucket s'il n'existe pas, et applique
 * une politique public-read si {@code public-read=true} (utile pour servir les
 * images directement depuis MinIO/Nginx sans signed URL).</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private final MinioProperties props;

    public MinioConfig(MinioProperties props) {
        this.props = props;
    }

    @Bean
    public MinioClient minioClient() {
        log.info("Initialisation client MinIO endpoint={}, bucket={}", props.endpoint(), props.bucket());
        if (props.accessKey() == null || props.accessKey().isBlank()
                || props.secretKey() == null || props.secretKey().isBlank()) {
            throw new IllegalStateException(
                    "MINIO_ACCESS_KEY et MINIO_SECRET_KEY doivent être définis quand app.storage.type=minio");
        }
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .region(props.region())
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBucket() {
        if (!props.autoCreateBucket()) {
            log.info("Auto-création du bucket désactivée (auto-create-bucket=false)");
            return;
        }
        MinioClient client = minioClient();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(props.bucket())
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(props.bucket())
                        .region(props.region())
                        .build());
                log.info("Bucket MinIO créé: {}", props.bucket());
            } else {
                log.debug("Bucket MinIO déjà existant: {}", props.bucket());
            }

            if (props.publicRead()) {
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [{
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                          }]
                        }""".formatted(props.bucket());
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(props.bucket())
                        .config(policy)
                        .build());
                log.info("Policy public-read appliquée sur le bucket {}", props.bucket());
            }
        } catch (Exception e) {
            // Non-fatal : on log l'erreur et on laisse l'admin la corriger
            log.error("Erreur d'initialisation du bucket MinIO '{}': {}", props.bucket(), e.getMessage(), e);
        }
    }
}
