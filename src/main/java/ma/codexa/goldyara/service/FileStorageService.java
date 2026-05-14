package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.service.storage.StorageService;
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
public class FileStorageService {

    private final StorageService storage;

    /**
     * Sauvegarde un fichier et retourne l'URL publique.
     */
    public String storeFile(MultipartFile file) throws IOException {
        try {
            return storage.store(file);
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
        return storage.storeAll(files);
    }

    /** Supprime un fichier précédemment stocké. */
    public void deleteFile(String urlOrName) {
        storage.delete(urlOrName);
    }
}
