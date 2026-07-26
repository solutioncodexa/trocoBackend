package ma.codexa.troco.service.storage;

import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.service.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local sur disque (par défaut sous {@code uploads/}).
 *
 * <p>Activé quand {@code app.storage.type=local} (ou absent — fallback).</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );
    static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf"
    );

    private final AuditLogService auditLog;
    private final ImageOptimizeService imageOptimizeService;
    private final String uploadDir;

    public LocalStorageService(AuditLogService auditLog,
                               ImageOptimizeService imageOptimizeService,
                               @Value("${app.upload.dir:uploads/}") String uploadDir) {
        this.auditLog = auditLog;
        this.imageOptimizeService = imageOptimizeService;
        this.uploadDir = uploadDir;
        log.info("Stockage LOCAL initialisé sur '{}'", uploadDir);
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

        try {
            OptimizedImage optimized = imageOptimizeService.optimize(file);
            if (!ALLOWED_CONTENT_TYPES.contains(optimized.contentType())) {
                throw new IllegalArgumentException("Type de fichier non autorisé après optimisation.");
            }
            String ext = optimized.extension();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                ext = resolveExtension(file.getOriginalFilename());
            }
            String filename = UUID.randomUUID() + ext;
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.write(target, optimized.bytes());
            String url = "/uploads/" + filename;
            log.info("Fichier sauvegardé localement: {} ({} bytes, optimized={})",
                    url, optimized.bytes().length, optimized.transformed());
            auditLog.log(AuditLogService.Action.FILE_UPLOAD, AuditLogService.Outcome.SUCCESS,
                    filename, "size=" + optimized.bytes().length + " ct=" + optimized.contentType()
                            + " optimized=" + optimized.transformed());
            return url;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            auditLog.log(AuditLogService.Action.FILE_UPLOAD, AuditLogService.Outcome.FAILURE,
                    file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
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
                log.warn("Échec upload d'un fichier: {}", e.getMessage());
            }
        }
        return urls;
    }

    @Override
    public void delete(String urlOrName) {
        if (urlOrName == null || urlOrName.isBlank()) return;
        String name = urlOrName.startsWith("/uploads/")
                ? urlOrName.substring("/uploads/".length())
                : urlOrName;
        try {
            Path p = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(name);
            if (Files.deleteIfExists(p)) {
                log.info("Fichier local supprimé: {}", p);
                auditLog.log(AuditLogService.Action.FILE_DELETE, AuditLogService.Outcome.SUCCESS, name);
            }
        } catch (IOException e) {
            auditLog.log(AuditLogService.Action.FILE_DELETE, AuditLogService.Outcome.FAILURE,
                    name, e.getMessage());
            log.warn("Échec suppression fichier {}: {}", name, e.getMessage());
        }
    }

    @Override
    public boolean isPublicAccessDirect() {
        return false;
    }

    static String resolveExtension(String originalFilename) {
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String extracted = originalFilename.substring(originalFilename.lastIndexOf('.'));
            if (ALLOWED_EXTENSIONS.contains(extracted.toLowerCase())) {
                ext = extracted.toLowerCase();
            }
        }
        return ext;
    }
}
