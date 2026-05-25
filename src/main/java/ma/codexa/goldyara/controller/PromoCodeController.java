package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.*;
import ma.codexa.goldyara.service.PromoCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promo-codes")
@RequiredArgsConstructor
@Tag(name = "Promo Codes", description = "Gestion des codes promo et règles automatiques")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    // ═══════════════════════════════════════════════════════════
    // Admin: Promo Codes CRUD
    // ═══════════════════════════════════════════════════════════

    @Operation(summary = "Lister tous les codes promo")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoCodeDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getAllPromoCodes()));
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
