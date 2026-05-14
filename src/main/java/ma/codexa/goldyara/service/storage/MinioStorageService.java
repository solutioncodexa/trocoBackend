package ma.codexa.goldyara.service.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.config.MinioProperties;
import ma.codexa.goldyara.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage objet via MinIO / S3-compatible.
 *
 * <p>Activé quand {@code app.storage.type=minio}.</p>
 *
 * <p>L'URL retournée est ABSOLUE et publique (avec le {@code public-endpoint}
 * de la config) — par ex. {@code https://files.goldyara.ma/goldyara-uploads/abcd.jpg}.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = LocalStorageService.ALLOWED_CONTENT_TYPES;

    private final MinioClient client;
    private final MinioProperties props;
    private final AuditLogService auditLog;

    public MinioStorageService(MinioClient client, MinioProperties props, AuditLogService auditLog) {
        this.client = client;
        this.props = props;
        this.auditLog = auditLog;
        log.info("Stockage MINIO initialisé : bucket={}, public={}",
                props.bucket(), props.effectivePublicEndpoint());
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez JPG, PNG, GIF, WebP ou PDF.");
        }

        String ext = LocalStorageService.resolveExtension(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            String url = buildPublicUrl(objectName);
            log.info("Fichier MinIO uploadé: {} ({})", url, file.getSize());
            auditLog.log(AuditLogService.Action.FILE_UPLOAD, AuditLogService.Outcome.SUCCESS,
                    objectName, "size=" + file.getSize() + " ct=" + contentType);
            return url;
        } catch (IOException | RuntimeException | java.security.NoSuchAlgorithmException
                 | java.security.InvalidKeyException
                 | io.minio.errors.MinioException e) {
            auditLog.log(AuditLogService.Action.FILE_UPLOAD, AuditLogService.Outcome.FAILURE,
                    objectName, e.getMessage());
            throw new RuntimeException("Erreur upload MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> storeAll(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        if (files == null) return urls;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                urls.add(store(file));
            } catch (Exception e) {
                log.warn("Échec upload MinIO d'un fichier: {}", e.getMessage());
            }
        }
        return urls;
    }

    @Override
    public void delete(String urlOrName) {
        if (urlOrName == null || urlOrName.isBlank()) return;
        String objectName = extractObjectName(urlOrName);
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectName)
                    .build());
            log.info("Objet MinIO supprimé: {}", objectName);
            auditLog.log(AuditLogService.Action.FILE_DELETE, AuditLogService.Outcome.SUCCESS, objectName);
        } catch (ErrorResponseException notFound) {
            // Idempotent : pas d'erreur si déjà supprimé
            log.debug("Objet MinIO déjà absent: {}", objectName);
        } catch (Exception e) {
            auditLog.log(AuditLogService.Action.FILE_DELETE, AuditLogService.Outcome.FAILURE,
                    objectName, e.getMessage());
            log.warn("Échec suppression MinIO {}: {}", objectName, e.getMessage());
        }
    }

    @Override
    public boolean isPublicAccessDirect() {
        return props.publicRead();
    }

    private String buildPublicUrl(String objectName) {
        String base = props.effectivePublicEndpoint().replaceAll("/+$", "");
        return base + "/" + props.bucket() + "/" + objectName;
    }

    private String extractObjectName(String urlOrName) {
        if (urlOrName == null) return "";
        String prefix = "/" + props.bucket() + "/";
        int idx = urlOrName.indexOf(prefix);
        if (idx >= 0) {
            return urlOrName.substring(idx + prefix.length());
        }
        // Sinon on suppose que c'est déjà le nom d'objet
        return urlOrName.startsWith("/") ? urlOrName.substring(1) : urlOrName;
    }
}
