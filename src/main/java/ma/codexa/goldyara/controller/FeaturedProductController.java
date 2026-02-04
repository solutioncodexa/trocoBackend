package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.FeaturedProductDTO;
import ma.codexa.goldyara.dto.request.CreateFeaturedProductRequest;
import ma.codexa.goldyara.dto.request.ReorderFeaturedProductsRequest;
import ma.codexa.goldyara.dto.request.ToggleFeaturedProductRequest;
import ma.codexa.goldyara.dto.request.UpdateFeaturedProductRequest;
import ma.codexa.goldyara.service.FeaturedProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/featured-products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Featured Products", description = "API de gestion des produits sélectionnés")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class FeaturedProductController {

    private final FeaturedProductService featuredProductService;

    @Operation(summary = "Récupérer tous les produits sélectionnés", description = "Récupère la liste de tous les produits sélectionnés actifs")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste des produits sélectionnés récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeaturedProductDTO>>> getAllFeaturedProducts() {
        log.debug("Récupération de tous les produits sélectionnés");
        List<FeaturedProductDTO> featuredProducts = featuredProductService.getAllFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success(featuredProducts));
    }

    @Operation(summary = "Récupérer les produits sélectionnés par section", description = "Récupère les produits sélectionnés pour une section spécifique")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produits sélectionnés récupérés avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Section invalide")
    })
    @GetMapping("/section/{section}")
    public ResponseEntity<ApiResponse<List<FeaturedProductDTO>>> getFeaturedProductsBySection(
            @Parameter(description = "Section (heritage ou sur-mesure)", required = true) 
            @PathVariable String section) {
        
        log.debug("Récupération des produits sélectionnés pour la section: {}", section);
        List<FeaturedProductDTO> featuredProducts = featuredProductService.getFeaturedProductsBySection(section);
        return ResponseEntity.ok(ApiResponse.success(featuredProducts));
    }

    @Operation(summary = "Récupérer un produit sélectionné par ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit sélectionné trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit sélectionné non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeaturedProductDTO>> getFeaturedProductById(
            @Parameter(description = "ID du produit sélectionné", required = true) @PathVariable Long id) {
        
        log.debug("Récupération du produit sélectionné avec l'id: {}", id);
        FeaturedProductDTO featuredProduct = featuredProductService.getFeaturedProductById(id);
        return ResponseEntity.ok(ApiResponse.success(featuredProduct));
    }

    @Operation(summary = "Créer un nouveau produit sélectionné")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Produit sélectionné créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Le produit est déjà sélectionné dans cette section")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<FeaturedProductDTO>> createFeaturedProduct(
            @Valid @RequestBody CreateFeaturedProductRequest request) {
        
        log.info("Création d'un nouveau produit sélectionné pour le produit ID: {}", request.getProductId());
        FeaturedProductDTO createdFeaturedProduct = featuredProductService.createFeaturedProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdFeaturedProduct, "Produit sélectionné créé avec succès"));
    }

    @Operation(summary = "Mettre à jour un produit sélectionné")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit sélectionné mis à jour avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit sélectionné non trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Le produit est déjà sélectionné dans cette section")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeaturedProductDTO>> updateFeaturedProduct(
            @Parameter(description = "ID du produit sélectionné", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateFeaturedProductRequest request) {
        
        log.info("Mise à jour du produit sélectionné avec l'id: {}", id);
        try {
            Long idLong = Long.parseLong(id);
            FeaturedProductDTO updatedFeaturedProduct = featuredProductService.updateFeaturedProduct(idLong, request);
            return ResponseEntity.ok(ApiResponse.success(updatedFeaturedProduct, "Produit sélectionné mis à jour avec succès"));
        } catch (NumberFormatException e) {
            log.error("ID invalide: {}", id);
            return ResponseEntity.badRequest().body(ApiResponse.error("ID invalide", 400));
        }
    }

    @Operation(summary = "Supprimer un produit sélectionné")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Produit sélectionné supprimé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit sélectionné non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeaturedProduct(
            @Parameter(description = "ID du produit sélectionné", required = true) @PathVariable String id) {
        
        log.info("Suppression du produit sélectionné avec l'id: {}", id);
        try {
            Long idLong = Long.parseLong(id);
            featuredProductService.deleteFeaturedProduct(idLong);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            log.error("ID invalide: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Réorganiser l'ordre des produits sélectionnés", description = "Réorganise l'ordre d'affichage des produits dans une section")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produits réorganisés avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderFeaturedProducts(
            @Valid @RequestBody ReorderFeaturedProductsRequest request) {
        
        log.info("Réorganisation des produits sélectionnés pour la section: {}", request.getSection());
        featuredProductService.reorderFeaturedProducts(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Produits réorganisés avec succès"));
    }

    @Operation(summary = "Activer/Désactiver un produit sélectionné", description = "Modifie le statut actif/inactif d'un produit sélectionné")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut modifié avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit sélectionné non trouvé")
    })
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<FeaturedProductDTO>> toggleFeaturedProduct(
            @Parameter(description = "ID du produit sélectionné", required = true) @PathVariable Long id,
            @Valid @RequestBody ToggleFeaturedProductRequest request) {
        
        log.info("Modification du statut du produit sélectionné avec l'id: {} - isActive: {}", id, request.getIsActive());
        FeaturedProductDTO updatedFeaturedProduct = featuredProductService.toggleFeaturedProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedFeaturedProduct, "Statut modifié avec succès"));
    }
}
