package ma.codexa.troco.repository;

import ma.codexa.troco.entity.Product;
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

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = FALSE")
    long countActive();

    /** Charge les produits avec images + catégorie (évite LazyInitializationException). */
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE")
    Page<Product> findAllWithImages(Pageable pageable);

    /** Variants sont chargés séparément dans le service (évite MultipleBagFetchException avec images). */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    /**
     * Charge le produit avec ses variantes (managed) pour la mise à jour.
     * Images chargées séparément via {@link #findByIdWithImages} (deux bags List en un seul JOIN FETCH).
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id AND p.deleted = FALSE")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);

    Optional<Product> findByExternalWooId(Long externalWooId);

    boolean existsByExternalWooId(Long externalWooId);

    // Filtrer par style
    List<Product> findByStyleAndDeletedFalse(String style);

    // Filtrer par catégorie
    List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    // Filtrer par plage de prix
    List<Product> findByPriceBetweenAndDeletedFalse(Double minPrice, Double maxPrice);

    // Produits en stock
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.deleted = FALSE")
    List<Product> findInStockProducts();

    // Recherche par nom (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.deleted = FALSE")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Filtres combinés (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:style IS NULL OR p.style = :style) AND " +
           "(:goldType IS NULL OR p.goldType = :goldType) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> findByFilters(
        @Param("style") String style,
        @Param("goldType") String goldType,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice
    );

    /** Filtres + recherche texte, paginé (images chargées pour les DTO). */
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(p.name) LIKE :keywordPattern OR LOWER(COALESCE(p.description, '')) LIKE :keywordPattern) AND " +
           "(:style IS NULL OR p.style = :style) AND " +
           "(:goldType IS NULL OR p.goldType = :goldType) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:inStock IS NULL OR (:inStock = TRUE AND p.stock > 0) OR (:inStock = FALSE AND p.stock <= 0))")
    Page<Product> searchProductsWithFilters(
        @Param("applyKeywordFilter") boolean applyKeywordFilter,
        @Param("keywordPattern") String keywordPattern,
        @Param("style") String style,
        @Param("goldType") String goldType,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("inStock") Boolean inStock,
        Pageable pageable
    );
}
