package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.common.PageResponse;
import ma.codexa.goldyara.common.constants.ApiConstants;
import ma.codexa.goldyara.common.exception.ResourceNotFoundException;
import ma.codexa.goldyara.dto.ProductDTO;
import ma.codexa.goldyara.dto.request.CreateProductRequest;
import ma.codexa.goldyara.entity.Category;
import ma.codexa.goldyara.entity.Image;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.mapper.ProductMapper;
import ma.codexa.goldyara.service.CategoryService;
import ma.codexa.goldyara.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ma.codexa.goldyara.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "API de gestion des produits")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Récupérer tous les produits", description = "Récupère une liste paginée de tous les produits")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste des produits récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Paramètres de pagination invalides")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDTO>>> getAllProducts(
            @Parameter(description = "Numéro de page (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Taille de la page", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @Parameter(description = "Champ de tri", example = "createdAt")
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_BY) String sortBy,
            @Parameter(description = "Direction de tri (ASC ou DESC)", example = "DESC")
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_DIRECTION) String sortDir) {

        log.debug("Récupération des produits - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productService.getAllProducts(pageable);
        PageResponse<ProductDTO> pageResponse = PageResponse.of(
                productMapper.toDTOList(productPage.getContent()),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "Récupérer un produit par ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit trouvé",
                    content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(
            @Parameter(description = "ID du produit", required = true) @PathVariable Long id) {

        log.debug("Récupération du produit avec l'id: {}", id);
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        return ResponseEntity.ok(ApiResponse.success(productMapper.toDTO(product)));
    }

    @Operation(summary = "Filtrer les produits")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> filterProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String goldType,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock) {

        log.debug("Filtrage des produits - category: {}, type: {}, goldType: {}", category, type, goldType);

        Long categoryId = (category != null && !category.isBlank())
                ? categoryService.getCategoryBySlug(category).map(Category::getId).orElse(null)
                : null;
        String style = (category != null && categoryId == null)
                ? (category.equalsIgnoreCase("beldi") ? "BELDI" : "MODERNE")
                : null;

        List<Product> products = productService.filterProducts(
                style, null, type != null ? type.toUpperCase() : null,
                categoryId, minPrice, maxPrice);

        List<Product> filteredProducts = products.stream()
                .filter(p -> collection == null || (p.getCollection() != null && p.getCollection().equals(collection)))
                .filter(p -> inStock == null || p.isInStock() == inStock)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(productMapper.toDTOList(filteredProducts)));
    }

    @Operation(summary = "Rechercher des produits par mot-clé")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success(productMapper.toDTOList(products)));
    }

    @Operation(summary = "Créer un nouveau produit avec images")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Produit créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {

        log.info("Création d'un nouveau produit: {}", request.getName());

        // Upload des images
        List<String> imageUrls = fileStorageService.storeFiles(images);
        if (imageUrls.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Au moins une image est requise", 400));
        }

        Product product = productMapper.toEntity(convertToDTO(request));
        product.setCategory(categoryService.getCategoryBySlug(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + request.getCategory())));
        product.setStyle(product.getCategory().getSlug().equalsIgnoreCase("beldi") ? "BELDI" : "MODERNE");
        setProductImages(product, imageUrls);
        Product createdProduct = productService.createProduct(product);

        log.info("Produit créé avec succès - ID: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        productMapper.toDTO(createdProduct),
                        ApiConstants.PRODUCT_CREATED));
    }

    @Operation(summary = "Mettre à jour un produit avec images")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit mis à jour avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @Parameter(description = "ID du produit", required = true) @PathVariable Long id,
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {

        log.info("Mise à jour du produit avec l'id: {}", id);

        Product existingProduct = productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));

        // Upload des nouvelles images si fournies
        List<String> imageUrls;
        if (images != null && images.length > 0 && !images[0].isEmpty()) {
            imageUrls = fileStorageService.storeFiles(images);
        } else {
            // Conserver les images existantes
            imageUrls = existingProduct.getImages().stream()
                    .map(img -> img.getUrl())
                    .toList();
        }

        ProductDTO productDTO = convertToDTO(request);
        productDTO.setId(id.toString());
        Product updatedProduct = productMapper.toEntity(productDTO);
        updatedProduct.setCategory(categoryService.getCategoryBySlug(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + request.getCategory())));
        updatedProduct.setStyle(updatedProduct.getCategory().getSlug().equalsIgnoreCase("beldi") ? "BELDI" : "MODERNE");
        setProductImages(updatedProduct, imageUrls);
        Product savedProduct = productService.updateProduct(id, updatedProduct);

        log.info("Produit mis à jour avec succès - ID: {}", savedProduct.getId());
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toDTO(savedProduct),
                ApiConstants.PRODUCT_UPDATED));
    }

    @Operation(summary = "Supprimer un produit")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Produit supprimé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID du produit", required = true) @PathVariable Long id) {

        log.info("Suppression du produit avec l'id: {}", id);
        productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        productService.deleteProduct(id);
        log.info("Produit supprimé avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    private ProductDTO convertToDTO(CreateProductRequest request) {
        ProductDTO dto = new ProductDTO();
        dto.setName(request.getName());
        dto.setDescription(request.getDescription());
        dto.setPrice(request.getPrice());
        dto.setOriginalPrice(request.getOriginalPrice());
        dto.setWeight(request.getWeight());
        dto.setCategory(request.getCategory());
        dto.setType(request.getType());
        dto.setGoldType(request.getGoldType());
        dto.setCollection(request.getCollection());
        dto.setAvailableSizes(request.getAvailableSizes());
        dto.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 1);
        dto.setMarginGain(request.getMarginGain());
        dto.setBadges(request.getBadges());
        return dto;
    }

    /** Crée les entités Image à partir des URLs et les attache au produit. */
    private void setProductImages(Product product, List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Image img = new Image();
            img.setUrl(urls.get(i));
            img.setProduct(product);
            img.setDisplayOrder(i);
            img.setIsPrimary(i == 0);
            images.add(img);
        }
        product.setImages(images);
    }
}
