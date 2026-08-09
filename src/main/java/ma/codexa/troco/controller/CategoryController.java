package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.BulkCategoryResultDTO;
import ma.codexa.troco.dto.CategoryCardDTO;
import ma.codexa.troco.dto.CategoryDTO;
import ma.codexa.troco.dto.CategoryHeroDTO;
import ma.codexa.troco.dto.CategoryNavDTO;
import ma.codexa.troco.dto.request.BulkCategoryActiveRequest;
import ma.codexa.troco.dto.request.BulkCategoryIdsRequest;
import ma.codexa.troco.dto.request.CreateCategoryRequest;
import ma.codexa.troco.dto.request.HeroCategoryPatchRequest;
import ma.codexa.troco.dto.request.UpdateCategoryRequest;
import ma.codexa.troco.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API des catégories de produits")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Liste des catégories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoryDtos()));
    }

    @Operation(summary = "Nav / footer — catégories légères (sans productCount)")
    @GetMapping("/nav")
    public ResponseEntity<ApiResponse<List<CategoryNavDTO>>> getNavCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getNavCategoryDtos()));
    }

    @Operation(summary = "Cartes vitrine (home / page builder) — sans productCount")
    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<List<CategoryCardDTO>>> getCardCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCardCategoryDtos()));
    }

    @Operation(summary = "Catégories affichées sur l’accueil (bandeau), triées")
    @GetMapping("/hero")
    public ResponseEntity<ApiResponse<List<CategoryHeroDTO>>> getHeroCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getHeroCategoryDtos()));
    }

    @Operation(summary = "Récupérer une catégorie par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryDtoById(id)
                .map(cat -> ResponseEntity.ok(ApiResponse.success(cat)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catégorie non trouvée", 404)));
    }

    @Operation(summary = "Récupérer une catégorie par slug")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryBySlug(@PathVariable String slug) {
        return categoryService.getCategoryDtoBySlug(slug)
                .map(cat -> ResponseEntity.ok(ApiResponse.success(cat)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catégorie non trouvée", 404)));
    }

    @Operation(summary = "Créer une catégorie")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDTO created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Catégorie créée"));
    }

    @Operation(summary = "Modifier une catégorie")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryDTO updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Catégorie modifiée"));
    }

    @Operation(summary = "Mettre à jour l’affichage hero (image, ordre, visible)")
    @PatchMapping("/{id}/hero")
    public ResponseEntity<ApiResponse<CategoryDTO>> patchHeroCategory(
            @PathVariable Long id,
            @RequestBody HeroCategoryPatchRequest patch) {
        CategoryDTO updated = categoryService.patchHeroCategory(id, patch);
        return ResponseEntity.ok(ApiResponse.success(updated, "Bandeau accueil mis à jour"));
    }

    @Operation(summary = "Supprimer une catégorie")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer plusieurs catégories (enfants d’abord)")
    @PostMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<BulkCategoryResultDTO>> bulkDelete(
            @Valid @RequestBody BulkCategoryIdsRequest request) {
        BulkCategoryResultDTO result = categoryService.deleteCategories(request.getIds());
        return ResponseEntity.ok(ApiResponse.success(result,
                result.getFailureCount() == 0 ? "Suppression terminée" : "Suppression partielle"));
    }

    @Operation(summary = "Activer / désactiver plusieurs catégories")
    @PostMapping("/bulk-active")
    public ResponseEntity<ApiResponse<BulkCategoryResultDTO>> bulkActive(
            @Valid @RequestBody BulkCategoryActiveRequest request) {
        BulkCategoryResultDTO result = categoryService.setCategoriesActive(
                request.getIds(), Boolean.TRUE.equals(request.getActive()));
        String msg = Boolean.TRUE.equals(request.getActive()) ? "Activation terminée" : "Désactivation terminée";
        return ResponseEntity.ok(ApiResponse.success(result,
                result.getFailureCount() == 0 ? msg : msg + " (partielle)"));
    }

    @Operation(summary = "Activer / désactiver une catégorie")
    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<CategoryDTO>> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.setCategoryActive(id, active),
                active ? "Catégorie activée" : "Catégorie désactivée"));
    }
}
