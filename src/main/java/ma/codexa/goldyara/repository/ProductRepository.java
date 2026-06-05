package ma.codexa.goldyara.repository;

import ma.codexa.goldyara.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Charge les produits avec leurs images (évite LazyInitializationException). */
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE")
    Page<Product> findAllWithImages(Pageable pageable);

    /** Variants sont chargés séparément dans le service (évite MultipleBagFetchException avec images). */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    /**
     * Charge le produit avec ses variantes (managed) pour la mise à jour.
     * Images chargées séparément via {@link #findByIdWithImages} (deux bags List en un seul JOIN FETCH).
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id AND p.deleted = FALSE")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);

    // Filtrer par style
    List<Product> findByStyleAndDeletedFalse(String style);

    // Filtrer par type d'or
    List<Product> findByGoldTypeAndDeletedFalse(String goldType);

    // Filtrer par type de produit
    List<Product> findByProductTypeAndDeletedFalse(String productType);

    // Filtrer par catégorie
    List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

    // Filtrer par plage de prix
    List<Product> findByPriceBetweenAndDeletedFalse(Double minPrice, Double maxPrice);

    // Produits en stock
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.deleted = FALSE")
    List<Product> findInStockProducts();

    // Recherche par nom (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.deleted = FALSE")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Filtres combinés (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:style IS NULL OR p.style = :style) AND " +
           "(:goldType IS NULL OR p.goldType = :goldType) AND " +
           "(:productType IS NULL OR p.productType = :productType) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> findByFilters(
        @Param("style") String style,
        @Param("goldType") String goldType,
        @Param("productType") String productType,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice
    );

    /** Filtres + recherche texte, paginé (images chargées pour les DTO). */
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(p.name) LIKE :keywordPattern OR LOWER(COALESCE(p.description, '')) LIKE :keywordPattern) AND " +
           "(:style IS NULL OR p.style = :style) AND " +
           "(:goldType IS NULL OR p.goldType = :goldType) AND " +
           "(:productType IS NULL OR p.productType = :productType) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:inStock IS NULL OR (:inStock = TRUE AND p.stock > 0) OR (:inStock = FALSE AND p.stock <= 0))")
    Page<Product> searchProductsWithFiltersWithoutCollection(
        @Param("applyKeywordFilter") boolean applyKeywordFilter,
        @Param("keywordPattern") String keywordPattern,
        @Param("style") String style,
        @Param("goldType") String goldType,
        @Param("productType") String productType,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("inStock") Boolean inStock,
        Pageable pageable
    );

    /**
     * Même périmètre mais filtre {@code collection} actif ({@code TRIM} / compare texte).
     * Si {@code collection} est en BYTEA en base, préférez corriger la colonne ({@code fix-products-collection-type.sql}).
     */
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(p.name) LIKE :keywordPattern OR LOWER(COALESCE(p.description, '')) LIKE :keywordPattern) AND " +
           "(:style IS NULL OR p.style = :style) AND " +
           "(:goldType IS NULL OR p.goldType = :goldType) AND " +
           "(:productType IS NULL OR p.productType = :productType) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:collectionFilter IS NULL OR (p.collection IS NOT NULL AND LOWER(TRIM(p.collection)) = LOWER(TRIM(:collectionFilter)))) AND " +
           "(:inStock IS NULL OR (:inStock = TRUE AND p.stock > 0) OR (:inStock = FALSE AND p.stock <= 0))")
    Page<Product> searchProductsWithFilters(
        @Param("applyKeywordFilter") boolean applyKeywordFilter,
        @Param("keywordPattern") String keywordPattern,
        @Param("style") String style,
        @Param("goldType") String goldType,
        @Param("productType") String productType,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("collectionFilter") String collectionFilter,
        @Param("inStock") Boolean inStock,
        Pageable pageable
    );
}
