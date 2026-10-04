package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.AdminStoreSummaryDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.request.UpdateStoreSettingsRequest;
import ma.codexa.troco.service.FournisseurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/store-settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final FournisseurService fournisseurService;

    /** Shell admin (layout / dashboard) — sans config complète. */
    @GetMapping("/me/summary")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<AdminStoreSummaryDTO>> meSummary() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.getMyStoreSummary()));
    }

    /** Config complète — page Paramètres uniquement. */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> me() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.getMyStoreSettings()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> update(
            @Valid @RequestBody UpdateStoreSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.updateMyStoreSettings(request)));
    }

    /** Lance la boutique (visible des clients) ou la remet en préparation. Corps : {"live": true}. */
    @PutMapping("/me/launch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminStoreSummaryDTO>> launch(@RequestBody java.util.Map<String, Boolean> body) {
        boolean live = Boolean.TRUE.equals(body.get("live"));
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.setStorefrontLive(live)));
    }

    @PostMapping("/me/verify-domain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> verifyDomain() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.verifyMyCustomDomain()));
    }
}
