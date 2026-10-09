package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.InstagramDraftDTO;
import ma.codexa.troco.dto.request.InstagramRequests;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.instagram.InstagramImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Import de produits depuis Instagram : brouillons à relire, puis publication en lot. */
@RestController
@RequestMapping("/instagram-import")
@RequiredArgsConstructor
@RequirePermission(AppPermissions.PRODUCTS_CREATE)
@Tag(name = "Instagram import", description = "Importer des produits depuis des posts Instagram")
public class InstagramImportController {

    private final InstagramImportService service;

    @Operation(summary = "Importer des liens de posts Instagram (un brouillon par post)")
    @PostMapping("/links")
    public ResponseEntity<ApiResponse<List<InstagramImportService.Item>>> importLinks(
            @RequestBody @Valid InstagramRequests.Links body) {
        return ResponseEntity.ok(ApiResponse.success(service.importLinks(body.urls())));
    }

    @Operation(summary = "Importer un post à partir de photos et d'une légende collée")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InstagramImportService.Item>> importUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "caption", required = false) String caption) {
        return ResponseEntity.ok(ApiResponse.success(service.importUpload(files, caption)));
    }

    @Operation(summary = "Brouillons en attente de relecture")
    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<InstagramDraftDTO>>> drafts() {
        return ResponseEntity.ok(ApiResponse.success(service.pending()));
    }

    @Operation(summary = "Modifier un brouillon avant publication")
    @PatchMapping("/drafts/{id}")
    public ResponseEntity<ApiResponse<InstagramDraftDTO>> update(
            @PathVariable Long id, @RequestBody @Valid InstagramRequests.DraftUpdate body) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, body)));
    }

    @Operation(summary = "Écarter un brouillon")
    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> discard(@PathVariable Long id) {
        service.discard(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publier des brouillons en lot")
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<List<InstagramImportService.PublishItem>>> publish(
            @RequestBody @Valid InstagramRequests.Publish body) {
        return ResponseEntity.ok(ApiResponse.success(service.publish(body)));
    }
}
