package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.FournisseurDTO;
import ma.codexa.troco.dto.PlanDTO;
import ma.codexa.troco.dto.StoreSettingsDTO;
import ma.codexa.troco.dto.StoreThemeDTO;
import ma.codexa.troco.dto.request.CreateFournisseurRequest;
import ma.codexa.troco.service.FournisseurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final FournisseurService fournisseurService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanDTO>>> plans() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.listPlans()));
    }

    @GetMapping("/themes")
    public ResponseEntity<ApiResponse<List<StoreThemeDTO>>> themes() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.listThemes()));
    }

    @GetMapping("/store")
    public ResponseEntity<ApiResponse<StoreSettingsDTO>> publicStore(
            @RequestParam(required = false) String slug) {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.getPublicStore(slug)));
    }

    /**
     * Inscription publique vendeur — boutique créée en PENDING (activation Super Admin).
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<FournisseurDTO>> registerStore(
            @Valid @RequestBody CreateFournisseurRequest request) {
        if (request.getPlanCode() == null || request.getPlanCode().isBlank()) {
            request.setPlanCode("basic");
        }
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.create(request, false)));
    }

    @GetMapping("/fournisseurs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<FournisseurDTO>>> listFournisseurs() {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.listAll()));
    }

    @GetMapping("/fournisseurs/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FournisseurDTO>> getFournisseur(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.getById(id)));
    }

    @PostMapping("/fournisseurs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FournisseurDTO>> createFournisseur(
            @Valid @RequestBody CreateFournisseurRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.create(request, true)));
    }

    @PatchMapping("/fournisseurs/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FournisseurDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(ApiResponse.success(fournisseurService.updateStatus(id, status)));
    }

    @PatchMapping("/fournisseurs/{id}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FournisseurDTO>> updatePlan(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                fournisseurService.updatePlan(id, body.get("planCode"))));
    }
}
