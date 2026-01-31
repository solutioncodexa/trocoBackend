package ma.codexa.goldyara.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf");

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    /**
     * Sauvegarde un fichier (image ou PDF) et retourne l'URL relative (/uploads/xxx.jpg).
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez JPG, PNG, GIF, WebP ou PDF.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String extractedExt = originalFilename.substring(originalFilename.lastIndexOf('.'));
            if (ALLOWED_EXTENSIONS.contains(extractedExt.toLowerCase())) {
                ext = extractedExt.toLowerCase();
            }
        }
        String filename = UUID.randomUUID().toString() + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        log.info("Fichier sauvegardé: {}", target);
        return "/uploads/" + filename;
    }

    /**
     * Sauvegarde plusieurs fichiers et retourne la liste des URLs.
     */
    public List<String> storeFiles(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        if (files == null) return urls;
        for (MultipartFile file : files) {
            try {
                urls.add(storeFile(file));
            } catch (Exception e) {
                log.warn("Échec upload d'un fichier: {}", e.getMessage());
            }
        }
        return urls;
    }
}
