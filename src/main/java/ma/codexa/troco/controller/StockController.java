package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.*;
import ma.codexa.troco.dto.request.BulkSafetyStockRequest;
import ma.codexa.troco.dto.request.StockAdjustRequest;
import ma.codexa.troco.dto.request.StockVariantPatchRequest;
import ma.codexa.troco.service.StockService;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stock", description = "Gestion de stock et alertes")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "Paramètres globaux de stock")
    @GetMapping("/settings")
    @RequirePermission(AppPermissions.STOCK_VIEW)
    public ResponseEntity<ApiResponse<StockSettingsDTO>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(stockService.getSettings()));
    }

    @Operation(summary = "Mettre à jour les paramètres globaux")
    @PutMapping("/settings")
    @RequirePermission(AppPermissions.STOCK_ADJUST)
    public ResponseEntity<ApiResponse<StockSettingsDTO>> updateSettings(@RequestBody StockSettingsDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(stockService.updateSettings(dto), "Paramètres mis à jour"));
    }

    @Operation(summary = "Vue d'ensemble stock")
    @GetMapping("/overview")
    @RequirePermission(AppPermissions.STOCK_VIEW)
    public ResponseEntity<ApiResponse<StockOverviewDTO>> overview() {
        return ResponseEntity.ok(ApiResponse.success(stockService.getOverview()));
    }

    @Operation(summary = "Liste des variantes avec stock (paginée)")
    @GetMapping("/variants")
    @RequirePermission(AppPermissions.STOCK_VIEW)
    public ResponseEntity<ApiResponse<PageResponse<StockVariantRowDTO>>> listVariants(
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.success(stockService.listVariantRows(filter, page, size)));
    }

    @Operation(summary = "Mouvements de stock (historique achats, ventes, commandes site…)")
    @GetMapping("/movements")
    @RequirePermission(AppPermissions.STOCK_VIEW)
    public ResponseEntity<ApiResponse<PageResponse<StockMovementDTO>>> movements(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<StockMovementDTO> result = stockService.listMovements(variantId, type, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements())));
    }

    @Operation(summary = "Ajuster le stock (achat, vente directe, correction inventaire)")
    @PostMapping("/adjust")
    @RequirePermission(AppPermissions.STOCK_ADJUST)
    public ResponseEntity<ApiResponse<StockVariantRowDTO>> adjust(
            @Valid @RequestBody StockAdjustRequest request,
            Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success(stockService.adjust(request, by), "Stock mis à jour"));
    }

    @Operation(summary = "Modifier seuil / péremption d'une variante")
    @PatchMapping("/variants/{id}")
    @RequirePermission(AppPermissions.STOCK_ADJUST)
    public ResponseEntity<ApiResponse<StockVariantRowDTO>> patchVariant(
            @PathVariable Long id,
            @RequestBody StockVariantPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.patchVariant(id, request)));
    }

    @Operation(summary = "Appliquer un seuil en masse")
    @PostMapping("/variants/bulk-safety")
    @RequirePermission(AppPermissions.STOCK_ADJUST)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkSafety(@RequestBody BulkSafetyStockRequest request) {
        int updated = stockService.bulkSafety(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated), "Seuils mis à jour"));
    }
}
