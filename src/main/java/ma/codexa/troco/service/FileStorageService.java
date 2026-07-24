package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.service.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Façade legacy : conserve l'API existante (storeFile / storeFiles) afin que
 * les contrôleurs déjà câblés ({@code ProductController}, {@code CustomOrderController})
 * continuent de fonctionner sans modification, mais délègue tout au backend
 * de stockage actif (local ou MinIO).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageService storage;

    /**
     * Sauvegarde un fichier et retourne l'URL publique.
     */
    public String storeFile(MultipartFile file) throws IOException {
        try {
            String url = storage.store(file);
            log.info("file_upload_single sizeBytes={}", file != null ? file.getSize() : 0);
            return url;
        } catch (RuntimeException e) {
            // Préserve la signature historique throws IOException pour les appelants
            if (e.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Sauvegarde plusieurs fichiers et retourne la liste des URLs.
     */
    public List<String> storeFiles(MultipartFile[] files) {
        List<String> urls = storage.storeAll(files);
        log.info("file_upload_batch multipartCount={} storedCount={}",
                files != null ? files.length : 0, urls.size());
        return urls;
    }

    /** Supprime un fichier précédemment stocké. */
    public void deleteFile(String urlOrName) {
        log.info("file_delete_requested");
        storage.delete(urlOrName);
    }
}
