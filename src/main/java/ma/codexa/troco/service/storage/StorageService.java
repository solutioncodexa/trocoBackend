package ma.codexa.troco.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Abstraction de stockage de fichiers utilisée par les contrôleurs.
 *
 * <p>Deux implémentations :</p>
 * <ul>
 *     <li>{@code LocalStorageService} — disque local (dev / fallback).</li>
 *     <li>{@code MinioStorageService} — bucket MinIO/S3 (prod).</li>
 * </ul>
 *
 * <p>L'URL retournée est une URL <strong>publique</strong> directement
 * affichable par le frontend. Pour le local elle est relative
 * ({@code /uploads/xxx.jpg}) ; pour MinIO elle est absolue.</p>
 */
public interface StorageService {

    /** Stocke un fichier et retourne son URL publique. */
    String store(MultipartFile file);

    /** Stocke plusieurs fichiers, ignore ceux qui échouent (logué). */
    List<String> storeAll(MultipartFile[] files);

    /** Supprime un fichier précédemment stocké via son URL ou son nom. */
    void delete(String urlOrName);

    /**
     * Indique si l'implémentation expose un endpoint public direct.
     * Utile pour le front afin de savoir s'il doit construire l'URL
     * lui-même ou non.
     */
    boolean isPublicAccessDirect();
}
