package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.CategoryDTO;
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

    @Operation(summary = "Catégories affichées sur l’accueil (bandeau), triées")
    @GetMapping("/hero")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getHeroCategories() {
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
}
