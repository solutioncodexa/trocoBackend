package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.ProductAttributeTemplateDTO;
import ma.codexa.troco.dto.request.SaveAttributeTemplateRequest;
import ma.codexa.troco.service.ProductAttributeTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Modèles d'attributs de variantes configurables par boutique / catégorie.
 * Endpoints back-office uniquement (voir règles SecurityConfig).
 */
@RestController
@RequestMapping("/attribute-templates")
@RequiredArgsConstructor
@Tag(name = "AttributeTemplates", description = "Modèles d'attributs de variantes par boutique/catégorie")
public class ProductAttributeTemplateController {

    private final ProductAttributeTemplateService templateService;

    @Operation(summary = "Liste des modèles d'attributs de la boutique")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductAttributeTemplateDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAll()));
    }

    @Operation(summary = "Modèle résolu pour une catégorie (fallback boutique)")
    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<ProductAttributeTemplateDTO>> resolve(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(templateService.resolve(categoryId)));
    }

    @Operation(summary = "Créer / mettre à jour un modèle d'attributs")
    @PutMapping
    public ResponseEntity<ApiResponse<ProductAttributeTemplateDTO>> save(
            @RequestBody SaveAttributeTemplateRequest request) {
        ProductAttributeTemplateDTO saved = templateService.save(request);
        return ResponseEntity.ok(ApiResponse.success(saved, "Modèle d'attributs enregistré"));
    }

    @Operation(summary = "Supprimer un modèle d'attributs")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
