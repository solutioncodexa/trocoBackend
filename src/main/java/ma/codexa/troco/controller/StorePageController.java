package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.StorePageAnalyticsSummaryDTO;
import ma.codexa.troco.dto.StorePageDTO;
import ma.codexa.troco.dto.StorePageNavDTO;
import ma.codexa.troco.dto.StorePageVersionDTO;
import ma.codexa.troco.dto.request.ReplaceStorePageBlocksRequest;
import ma.codexa.troco.dto.request.TrackPageAnalyticsRequest;
import ma.codexa.troco.dto.request.UpsertStorePageRequest;
import ma.codexa.troco.service.StorePageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store-pages")
@RequiredArgsConstructor
public class StorePageController {

    private final StorePageService storePageService;

    @GetMapping("/public/nav")
    public ResponseEntity<ApiResponse<List<StorePageNavDTO>>> publicNav(
            @RequestParam(required = false, defaultValue = "fr") String lang) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.listPublicNav(lang)));
    }

    @GetMapping("/public/homes")
    public ResponseEntity<ApiResponse<List<StorePageDTO>>> publicHomes(
            @RequestParam(required = false, defaultValue = "fr") String lang) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.listPublicHomes(lang)));
    }

    @GetMapping("/public/home")
    public ResponseEntity<ApiResponse<?>> publicHome(
            @RequestParam(required = false, defaultValue = "fr") String lang,
            @RequestParam(required = false) String variant) {
        return storePageService.getPublicHome(lang, variant)
                .<ResponseEntity<ApiResponse<?>>>map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/public/by-slug/{slug}")
    public ResponseEntity<ApiResponse<StorePageDTO>> publicBySlug(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "fr") String lang) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.getPublicBySlug(slug, lang)));
    }

    @GetMapping("/public/preview/{token}")
    public ResponseEntity<ApiResponse<StorePageDTO>> publicPreview(
            @PathVariable String token,
            @RequestParam(required = false, defaultValue = "fr") String lang) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.getPublicPreview(token, lang)));
    }

    @PostMapping("/public/track")
    public ResponseEntity<ApiResponse<Void>> track(@Valid @RequestBody TrackPageAnalyticsRequest request) {
        storePageService.track(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/public/block-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> publicBlockTypes() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("type", "hero", "label", "Hero / Bannière"),
                Map.of("type", "rich_text", "label", "Texte"),
                Map.of("type", "products", "label", "Grille produits"),
                Map.of("type", "categories", "label", "Catégories"),
                Map.of("type", "cta", "label", "Appel à l’action"),
                Map.of("type", "image", "label", "Image"),
                Map.of("type", "faq", "label", "FAQ"),
                Map.of("type", "spacer", "label", "Espace"),
                Map.of("type", "contact", "label", "Formulaire contact"),
                Map.of("type", "video", "label", "Vidéo"),
                Map.of("type", "testimonials", "label", "Témoignages"),
                Map.of("type", "countdown", "label", "Compteur promo"),
                Map.of("type", "instagram", "label", "Grille Instagram")
        )));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StorePageDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(storePageService.listAdmin()));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StorePageAnalyticsSummaryDTO>>> analytics(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.analyticsSummary(days)));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> importPage(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.importPage(payload), "Page importée"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.getAdmin(id)));
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> export(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.exportPage(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> create(@Valid @RequestBody UpsertStorePageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.create(request), "Page créée"));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> clone(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.clonePage(id), "Page dupliquée"));
    }

    @PostMapping("/{id}/preview-link")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, String>>> previewLink(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.issuePreviewToken(id)));
    }

    @PostMapping("/{id}/preview-link/rotate")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, String>>> rotatePreviewLink(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.rotatePreviewToken(id)));
    }

    @PostMapping("/{id}/promote-ab")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> promoteAb(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                storePageService.promoteAbWinner(id), "Variante A/B promue comme accueil unique"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertStorePageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.update(id, request), "Page mise à jour"));
    }

    @PutMapping("/{id}/blocks")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StorePageDTO>> replaceBlocks(
            @PathVariable Long id,
            @Valid @RequestBody ReplaceStorePageBlocksRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.replaceBlocks(id, request), "Composants enregistrés"));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StorePageVersionDTO>>> versions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.listVersions(id)));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StorePageDTO>> restore(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(storePageService.restoreVersion(id, versionId), "Version restaurée"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storePageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
