package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.service.storage.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Endpoints d'upload — délèguent au {@link StorageService} actif (local/MinIO).
 *
 * <p>L'accès est restreint à ROLE_ADMIN par {@code SecurityConfig}.</p>
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Upload de fichiers (images, PDF)")
public class UploadController {

    private final StorageService storage;

    @Operation(summary = "Upload d'un fichier unique")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = storage.store(file);
            return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), 400));
        } catch (RuntimeException e) {
            log.error("Erreur upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de l'enregistrement du fichier", 500));
        }
    }

    @Operation(summary = "Upload multiple")
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) {
        List<String> urls = storage.storeAll(files);
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

}
