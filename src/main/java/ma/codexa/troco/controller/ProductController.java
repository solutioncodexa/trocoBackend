package ma.codexa.troco.controller;

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
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.common.constants.ApiConstants;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.ProductDetailDTO;
import ma.codexa.troco.dto.ProductListItemDTO;
import ma.codexa.troco.dto.request.CreateProductRequest;
import ma.codexa.troco.entity.Category;
import ma.codexa.troco.entity.Image;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.mapper.MapperUtils;
import ma.codexa.troco.mapper.ProductDtoMapper;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.service.CategoryService;
import ma.codexa.troco.service.ProductService;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ma.codexa.troco.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "API de gestion des produits")
public class ProductController {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "price", "weight", "name", "id"
    );

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductDtoMapper productDtoMapper;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final ma.codexa.troco.service.FeaturedProductService featuredProductService;
    private final ma.codexa.troco.service.ProductUpsellService productUpsellService;

    @GetMapping("/{id}/frequently-bought")
    public ResponseEntity<ApiResponse<List<ProductListItemDTO>>> frequentlyBought(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") @Min(1) @Max(12) int limit) {
        return ResponseEntity.ok(ApiResponse.success(productUpsellService.frequentlyBoughtWith(id, limit)));
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Recommandations produits (co-achat + catégorie)")
    public ResponseEntity<ApiResponse<List<ProductListItemDTO>>> recommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int limit) {
        return ResponseEntity.ok(ApiResponse.success(productUpsellService.frequentlyBoughtWith(id, limit)));
    }

    @GetMapping("/facets")
    @Operation(summary = "Facettes catalogue (catégories, tailles, bornes de prix)")
    public ResponseEntity<ApiResponse<ma.codexa.troco.dto.CatalogFacetsDTO>> facets() {
        return ResponseEntity.ok(ApiResponse.success(productService.catalogFacets()));
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Produits par IDs (wishlist) — DTO liste, max 50")
    public ResponseEntity<ApiResponse<List<ProductListItemDTO>>> getByIds(
            @RequestParam List<Long> ids) {
        List<Product> products = productService.getProductsByIds(ids);
        return ResponseEntity.ok(ApiResponse.success(productMapper.toListItemDTOList(products)));
    }

    @Operation(summary = "Récupérer tous les produits", description = "Récupère une liste paginée de tous les produits")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liste des produits récupérée avec succès",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Paramètres de pagination invalides")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListItemDTO>>> getAllProducts(
            @Parameter(description = "Numéro de page (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Taille de la page", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @Parameter(description = "Champ de tri", example = "createdAt")
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_BY) String sortBy,
            @Parameter(description = "Direction de tri (ASC ou DESC)", example = "DESC")
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String goldType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String facetSize) {

        log.debug("Récupération des produits - page: {}, size: {}, sortBy: {}, sortDir: {}, filtres actifs",
                page, size, sortBy, sortDir);

        Page<Product> productPage = loadProductPage(page, size, sortBy, sortDir,
                category, goldType, minPrice, maxPrice, inStock, keyword, facetSize);

        PageResponse<ProductListItemDTO> pageResponse = PageResponse.of(
                productMapper.toListItemDTOList(productPage.getContent()),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "Catalogue avec fiches complètes", description = "Mêmes filtres et pagination que GET /products ; DTO détail (admin, outils internes)")
    @GetMapping("/full-page")
    public ResponseEntity<ApiResponse<PageResponse<ProductDetailDTO>>> getAllProductsFullPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = ApiConstants.DEFAULT_SORT_DIRECTION) String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String goldType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String facetSize) {

        Page<Product> productPage = loadProductPage(page, size, sortBy, sortDir,
                category, goldType, minPrice, maxPrice, inStock, keyword, facetSize);

        PageResponse<ProductDetailDTO> pageResponse = PageResponse.of(
                productDtoMapper.toDetailDTOList(productPage.getContent()),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "Récupérer un produit par ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit trouvé",
                    content = @Content(schema = @Schema(implementation = ProductDetailDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductById(
            @Parameter(description = "ID du produit", required = true) @PathVariable Long id) {

        log.debug("Récupération du produit avec l'id: {}", id);
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toDetailDTO(product)));
    }

    @Operation(summary = "Filtrer les produits")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<ProductListItemDTO>>> filterProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String goldType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock) {

        log.debug("Filtrage des produits - category: {}, goldType: {}", category, goldType);

        Long categoryId = (category != null && !category.isBlank())
                ? categoryService.getCategoryBySlug(category).map(Category::getId).orElse(null)
                : null;
        String style = null;

        String goldTypeBackend = (goldType != null && !goldType.isBlank())
                ? MapperUtils.goldTypeToBackend(goldType)
                : null;

        List<Product> products = productService.filterProducts(
                style,
                goldTypeBackend,
                categoryId, minPrice, maxPrice);

        List<Product> filteredProducts = products.stream()
                .filter(p -> inStock == null || p.isInStock() == inStock)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(productMapper.toListItemDTOList(filteredProducts)));
    }

    @Operation(summary = "Rechercher des produits par mot-clé")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductListItemDTO>>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success(productMapper.toListItemDTOList(products)));
    }

    @Operation(summary = "Rechercher des produits par mot-clé")
    @GetMapping("/featured-test")
    public ResponseEntity<Map<String, String>> featuredTest() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Featured test endpoint works!");
        return ResponseEntity.ok(response);
    }

    // Endpoint simple pour les produits sélectionnés (compatibilité frontend)
    @GetMapping("/featured-products-mock")
    public ResponseEntity<List<Object>> getFeaturedProductsMock() {
        log.info("📡 Featured products mock endpoint called in ProductController");
        try {
            List<ma.codexa.troco.dto.FeaturedProductDTO> products = featuredProductService.getAllFeaturedProducts();
            log.info("✅ Returning {} featured products from database", products.size());
            
            // Enrichir avec les données complètes du produit
            List<Object> enrichedProducts = products.stream().map(fp -> {
                Map<String, Object> enrichedProduct = new HashMap<>();
                
                // Données du featured product
                enrichedProduct.put("id", fp.getId());
                enrichedProduct.put("productId", fp.getProductId());
                enrichedProduct.put("section", fp.getSection());
                enrichedProduct.put("displayOrder", fp.getDisplayOrder());
                enrichedProduct.put("isActive", fp.getIsActive());
                enrichedProduct.put("createdAt", fp.getCreatedAt());
                enrichedProduct.put("updatedAt", fp.getUpdatedAt());
                
                // Utiliser les données personnalisées ou celles du produit par défaut
                enrichedProduct.put("title", fp.getTitle() != null && !fp.getTitle().trim().isEmpty() 
                    ? fp.getTitle() : null);
                enrichedProduct.put("description", fp.getDescription() != null && !fp.getDescription().trim().isEmpty() 
                    ? fp.getDescription() : null);
                enrichedProduct.put("imageUrl", fp.getImageUrl() != null && !fp.getImageUrl().trim().isEmpty() 
                    ? fp.getImageUrl() : null);
                
                // Récupérer les données du produit pour les fallbacks
                try {
                    Product product = productService.getProductById(fp.getProductId()).orElse(null);
                    if (product != null) {
                        Map<String, Object> productInfo = new HashMap<>();
                        productInfo.put("id", product.getId());
                        productInfo.put("name", product.getName());
                        productInfo.put("description", product.getDescription());
                        productInfo.put("price", product.getPrice());
                        productInfo.put("goldType", product.getGoldType());
                        
                        // Image du produit
                        if (!product.getImages().isEmpty()) {
                            String productImageUrl = product.getImages().get(0).getUrl();
                            productInfo.put("imageUrl", productImageUrl);
                            
                            // Si pas d'image personnalisée, utiliser celle du produit
                            if (fp.getImageUrl() == null || fp.getImageUrl().trim().isEmpty()) {
                                enrichedProduct.put("imageUrl", productImageUrl);
                            }
                        }
                        
                        enrichedProduct.put("product", productInfo);
                        
                        // Si pas de titre personnalisé, utiliser le nom du produit
                        if (fp.getTitle() == null || fp.getTitle().trim().isEmpty()) {
                            enrichedProduct.put("title", product.getName());
                        }
                        
                        // Si pas de description personnalisée, utiliser celle du produit
                        if (fp.getDescription() == null || fp.getDescription().trim().isEmpty()) {
                            enrichedProduct.put("description", product.getDescription());
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Could not fetch product {} for featured product {}: {}", fp.getProductId(), fp.getId(), e.getMessage());
                }
                
                return enrichedProduct;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(enrichedProducts);
        } catch (Exception e) {
            log.error("❌ Error fetching featured products from database, returning empty list", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // Endpoint temporaire pour créer des produits sélectionnés (compatibilité frontend)
    @PostMapping("/featured-products")
    public ResponseEntity<Object> createFeaturedProduct(@RequestBody Object request) {
        log.info("📝 Create featured product endpoint called in ProductController");
        log.info("📝 Request data: {}", request);
        
        if (request instanceof Map) {
            Map<String, Object> requestData = (Map<String, Object>) request;
            
            try {
                // Convertir la requête en DTO
                ma.codexa.troco.dto.request.CreateFeaturedProductRequest createRequest = new ma.codexa.troco.dto.request.CreateFeaturedProductRequest();
                String productIdStr = (String) requestData.get("productId");
                createRequest.setProductId(Long.parseLong(productIdStr)); // Conversion String → Long
                createRequest.setSection((String) requestData.get("section"));
                createRequest.setTitle((String) requestData.get("title"));
                createRequest.setDescription((String) requestData.get("description"));
                
                // Récupérer l'image personnalisée ou utiliser celle du produit
                String customImageUrl = (String) requestData.get("imageUrl");
                if (customImageUrl == null || customImageUrl.trim().isEmpty()) {
                    // Si aucune image personnalisée, utiliser l'image du produit
                    Product product = productService.getProductById(Long.parseLong(productIdStr))
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + productIdStr));
                    
                    if (!product.getImages().isEmpty()) {
                        customImageUrl = product.getImages().get(0).getUrl();
                        log.info("📸 Using product image as featured product image: {}", customImageUrl);
                    } else {
                        customImageUrl = "/api/uploads/placeholder.jpg";
                        log.info("📸 Using placeholder image (product has no images)");
                    }
                }
                createRequest.setImageUrl(customImageUrl);
                
                // Utiliser le titre personnalisé ou celui du produit
                String customTitle = (String) requestData.get("title");
                if (customTitle == null || customTitle.trim().isEmpty()) {
                    Product product = productService.getProductById(Long.parseLong(productIdStr))
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + productIdStr));
                    customTitle = product.getName();
                    log.info("📝 Using product name as featured product title: {}", customTitle);
                }
                createRequest.setTitle(customTitle);
                
                // Utiliser la description personnalisée ou celle du produit
                String customDescription = (String) requestData.get("description");
                if (customDescription == null || customDescription.trim().isEmpty()) {
                    Product product = productService.getProductById(Long.parseLong(productIdStr))
                            .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + productIdStr));
                    customDescription = product.getDescription();
                    log.info("📝 Using product description as featured product description: {}", customDescription);
                }
                createRequest.setDescription(customDescription);
                
                createRequest.setDisplayOrder(1); // Sera ajusté par le service
                createRequest.setIsActive(true);
                
                // Utiliser le vrai service pour créer en base de données
                ma.codexa.troco.dto.FeaturedProductDTO createdProduct = featuredProductService.createFeaturedProduct(createRequest);
                
                log.info("✅ Featured product created in database with ID: {}", createdProduct.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
                
            } catch (Exception e) {
                log.error("❌ Error creating featured product in database: {}", e.getMessage(), e);
                return ResponseEntity.badRequest().body("Error creating featured product: " + e.getMessage());
            }
        }
        
        return ResponseEntity.badRequest().body("Invalid request data");
    }

    @Operation(summary = "Créer un nouveau produit avec images")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Produit créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(AppPermissions.PRODUCTS_CREATE)
    public ResponseEntity<ApiResponse<ProductDetailDTO>> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {

        log.info("Création d'un nouveau produit: {}", request.getName());

        // Upload des images
        List<String> imageUrls = fileStorageService.storeFiles(images);
        if (imageUrls.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Au moins une image est requise", 400));
        }

        Product product = productMapper.toEntity(convertToDetailDTO(request));
        product.setCategory(categoryService.getCategoryBySlug(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + request.getCategory())));
        product.setShortDescription(request.getShortDescription());
        product.setSku(request.getSku());
        product.setShowWeight(false);
        product.setCustomizable(Boolean.TRUE.equals(request.getCustomizable()));
        setProductImages(product, imageUrls);
        Product createdProduct = productService.createProduct(product, request.getVariants());

        log.info("Produit créé avec succès - ID: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        productDtoMapper.toDetailDTO(createdProduct),
                        ApiConstants.PRODUCT_CREATED));
    }

    @Operation(summary = "Mettre à jour un produit avec images")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Produit mis à jour avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(AppPermissions.PRODUCTS_UPDATE)
    public ResponseEntity<ApiResponse<ProductDetailDTO>> updateProduct(
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

        ProductDetailDTO productDTO = convertToDetailDTO(request);
        productDTO.setId(id.toString());
        Product updatedProduct = productMapper.toEntity(productDTO);
        updatedProduct.setCategory(categoryService.getCategoryBySlug(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée: " + request.getCategory())));
        updatedProduct.setShortDescription(request.getShortDescription());
        updatedProduct.setSku(request.getSku());
        updatedProduct.setShowWeight(false);
        updatedProduct.setCustomizable(Boolean.TRUE.equals(request.getCustomizable()));
        setProductImages(updatedProduct, imageUrls);
        Product savedProduct = productService.updateProduct(id, updatedProduct, request.getVariants());

        log.info("Produit mis à jour avec succès - ID: {}", savedProduct.getId());
        return ResponseEntity.ok(ApiResponse.success(
                productDtoMapper.toDetailDTO(savedProduct),
                ApiConstants.PRODUCT_UPDATED));
    }

    @Operation(summary = "Supprimer un produit")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Produit supprimé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @DeleteMapping("/{id}")
    @RequirePermission(AppPermissions.PRODUCTS_DELETE)
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID du produit", required = true) @PathVariable Long id) {

        log.info("Suppression du produit avec l'id: {}", id);
        productService.getProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", id));
        productService.deleteProduct(id);
        log.info("Produit supprimé avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    private Page<Product> loadProductPage(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String category,
            String goldType,
            Double minPrice,
            Double maxPrice,
            Boolean inStock,
            String keyword,
            String facetSize) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : ApiConstants.DEFAULT_SORT_BY;
        String safeSortDir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        if (!safeSortBy.equals(sortBy)) {
            log.warn("Champ de tri non supporté: '{}'. Fallback vers '{}'.", sortBy, safeSortBy);
        }
        if (!safeSortDir.equalsIgnoreCase(sortDir)) {
            log.warn("Direction de tri non supportée: '{}'. Fallback vers '{}'.", sortDir, safeSortDir);
        }

        Sort sort = "ASC".equalsIgnoreCase(safeSortDir)
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String sizeFacet = (facetSize != null && !facetSize.isBlank()) ? facetSize.trim() : null;
        boolean useFilters = kw != null
                || (category != null && !category.isBlank())
                || (goldType != null && !goldType.isBlank())
                || minPrice != null
                || maxPrice != null
                || Boolean.TRUE.equals(inStock)
                || sizeFacet != null;

        if (useFilters) {
            Long categoryId = (category != null && !category.isBlank())
                    ? categoryService.getCategoryBySlug(category).map(Category::getId).orElse(null)
                    : null;
            String style = null;
            String goldTypeBackend = (goldType != null && !goldType.isBlank())
                    ? MapperUtils.goldTypeToBackend(goldType)
                    : null;

            return productService.searchProductsWithFilters(
                    kw,
                    style,
                    goldTypeBackend,
                    categoryId,
                    minPrice,
                    maxPrice,
                    inStock,
                    sizeFacet,
                    pageable);
        }
        return productService.getAllProducts(pageable);
    }

    private ProductDetailDTO convertToDetailDTO(CreateProductRequest request) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setName(request.getName());
        dto.setDescription(request.getDescription());
        dto.setShortDescription(request.getShortDescription());
        dto.setPrice(request.getPrice());
        dto.setOriginalPrice(request.getOriginalPrice());
        dto.setCategory(request.getCategory());
        dto.setSku(request.getSku());
        dto.setGoldType(request.getGoldType());
        dto.setAvailableSizes(request.getAvailableSizes());
        dto.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        dto.setMarginGain(request.getMarginGain());
        dto.setWeight(request.getWeight());
        dto.setBadges(request.getBadges());
        dto.setShowWeight(false);
        dto.setCustomizable(Boolean.TRUE.equals(request.getCustomizable()));
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            dto.setVariants(request.getVariants().stream().map(v -> {
                ma.codexa.troco.dto.ProductVariantDTO vd = new ma.codexa.troco.dto.ProductVariantDTO();
                vd.setId(v.getId());
                vd.setAttributeName(v.getAttributeName());
                vd.setAttributeValue(v.getAttributeValue());
                vd.setLabel(v.getLabel());
                vd.setPrice(v.getPrice());
                vd.setOriginalPrice(v.getOriginalPrice());
                vd.setStock(v.getStock());
                vd.setSafetyStock(v.getSafetyStock());
                vd.setReorderQty(v.getReorderQty());
                vd.setExpiryDate(v.getExpiryDate());
                vd.setSku(v.getSku());
                vd.setDisplayOrder(v.getDisplayOrder());
                vd.setIsDefault(v.getIsDefault());
                vd.setWeight(v.getWeight());
                vd.setMarginGain(v.getMarginGain());
                return vd;
            }).toList());
            request.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                    .findFirst()
                    .or(() -> request.getVariants().stream().findFirst())
                    .ifPresent(def -> {
                        if (def.getPrice() != null) {
                            dto.setPrice(def.getPrice());
                        }
                        if (def.getStock() != null) {
                            dto.setStockQuantity(def.getStock());
                        }
                    });
        }
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
