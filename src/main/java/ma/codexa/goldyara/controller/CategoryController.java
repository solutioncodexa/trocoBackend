package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.request.HeroCategoryPatchRequest;
import ma.codexa.goldyara.entity.Category;
import ma.codexa.goldyara.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API des catégories de produits")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Liste des catégories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @Operation(summary = "Catégories affichées sur l’accueil (bandeau), triées")
    @GetMapping("/hero")
    public ResponseEntity<ApiResponse<List<Category>>> getHeroCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getHeroCategories()));
    }

    @Operation(summary = "Récupérer une catégorie par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(cat -> ResponseEntity.ok(ApiResponse.success(cat)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catégorie non trouvée", 404)));
    }

    @Operation(summary = "Récupérer une catégorie par slug")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<Category>> getCategoryBySlug(@PathVariable String slug) {
        return categoryService.getCategoryBySlug(slug)
                .map(cat -> ResponseEntity.ok(ApiResponse.success(cat)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catégorie non trouvée", 404)));
    }

    @Operation(summary = "Créer une catégorie")
    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdCategory, "Catégorie créée"));
    }

    @Operation(summary = "Modifier une catégorie")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @RequestBody Category categoryDetails) {
        Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(ApiResponse.success(updatedCategory, "Catégorie modifiée"));
    }

    @Operation(summary = "Mettre à jour l’affichage hero (image, ordre, visible)")
    @PatchMapping("/{id}/hero")
    public ResponseEntity<ApiResponse<Category>> patchHeroCategory(
            @PathVariable Long id,
            @RequestBody HeroCategoryPatchRequest patch) {
        Category updated = categoryService.patchHeroCategory(id, patch);
        return ResponseEntity.ok(ApiResponse.success(updated, "Bandeau accueil mis à jour"));
    }

    @Operation(summary = "Supprimer une catégorie")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
