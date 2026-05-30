package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.common.PageResponse;
import ma.codexa.goldyara.dto.*;
import ma.codexa.goldyara.service.PromoCodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promo-codes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Promo Codes", description = "Gestion des codes promo et règles automatiques")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    // ═══════════════════════════════════════════════════════════
    // Admin: Promo Codes CRUD
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Lister les codes promo (paginé)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromoCodeDTO>>> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String keyword) {
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<PromoCodeDTO> promoPage = promoCodeService.getPromoCodesPage(keyword, PageRequest.of(page, size, sort));
        PageResponse<PromoCodeDTO> pageResponse = PageResponse.of(
                promoPage.getContent(),
                promoPage.getNumber(),
                promoPage.getSize(),
                promoPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "Statistiques des codes promo")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PromoCodeStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getPromoCodeStats()));
    }

    @Operation(summary = "Récupérer un code promo par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getPromoCodeById(id)));
    }

    @Operation(summary = "Créer un code promo")
    @PostMapping
    public ResponseEntity<ApiResponse<PromoCodeDTO>> create(@Valid @RequestBody CreatePromoCodeRequest req) {
        PromoCodeDTO created = promoCodeService.createPromoCode(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Code promo créé"));
    }

    @Operation(summary = "Modifier un code promo")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromoCodeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.updatePromoCode(id, req)));
    }

    @Operation(summary = "Supprimer un code promo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activer/désactiver un code promo")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.toggleActive(id, isActive)));
    }

    @Operation(summary = "Générer un code aléatoire unique")
    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateCode() {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.generateCode()));
    }

    // ═══════════════════════════════════════════════════════════
    // Public: Validate promo code at checkout
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Valider un code promo (public)")
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ValidatePromoCodeResponse>> validate(
            @RequestParam String code,
            @RequestParam double orderTotal) {
        ValidatePromoCodeResponse res = promoCodeService.validateCode(code, orderTotal);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // ═══════════════════════════════════════════════════════════
    // Public: Promo suggestions for checkout
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Suggestions de codes promo selon le panier (public)")
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<PromoSuggestionDTO>>> getSuggestions(
            @RequestParam double orderTotal) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getSuggestions(orderTotal)));
    }

    @Operation(summary = "Liste des codes promo publics actifs")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PublicPromoCodeDTO>>> getPublicCodes() {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getPublicCodes()));
    }
}
