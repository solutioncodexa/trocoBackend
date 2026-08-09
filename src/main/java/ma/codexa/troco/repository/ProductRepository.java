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

    @Query("SELECT p.id FROM Product p WHERE p.deleted = FALSE ORDER BY p.id")
    List<Long> findAllActiveIds();

    /** Charge les produits avec images + catégorie (évite LazyInitializationException). */
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE")
    Page<Product> findAllWithImages(Pageable pageable);

    /** Variants sont chargés séparément dans le service (évite MultipleBagFetchException avec images). */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = FALSE AND p.id IN :ids")
    List<Product> findByIdInWithImages(@Param("ids") List<Long> ids);

    /**
     * Charge le produit avec ses variantes (managed) pour la mise à jour.
     * Images chargées séparément via {@link #findByIdWithImages} (deux bags List en un seul JOIN FETCH).
     */
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id AND p.deleted = FALSE")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);

    Optional<Product> findByExternalWooId(Long externalWooId);

    boolean existsByExternalWooId(Long externalWooId);

    // Filtrer par catégorie
    List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    @Query("""
            SELECT p.category.id, COUNT(p)
            FROM Product p
            WHERE p.deleted = FALSE AND p.category IS NOT NULL
            GROUP BY p.category.id
            """)
    List<Object[]> countActiveGroupedByCategoryId();

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
           // Ne pas LOWER(:param) : Hibernate peut lier null en bytea → lower(bytea) sous Postgres
           "(:marque IS NULL OR LOWER(p.marque) = :marque) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> findByFilters(
        @Param("marque") String marque,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice
    );

    /** Filtres + recherche texte, paginé (images chargées pour les DTO). */
    @EntityGraph(attributePaths = {"images", "category"})
    @Query("SELECT p FROM Product p WHERE " +
           "p.deleted = FALSE AND " +
           "(:applyKeywordFilter = FALSE OR LOWER(p.name) LIKE :keywordPattern OR " +
           "(p.description IS NOT NULL AND LOWER(p.description) LIKE :keywordPattern)) AND " +
           "(:marque IS NULL OR LOWER(p.marque) = :marque) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:inStock IS NULL OR (:inStock = TRUE AND p.stock > 0) OR (:inStock = FALSE AND p.stock <= 0)) AND " +
           "(:size IS NULL OR EXISTS (SELECT 1 FROM ProductVariant v WHERE v.product = p AND (" +
           "(v.attributeValue IS NOT NULL AND LOWER(v.attributeValue) = :size) OR " +
           "(v.label IS NOT NULL AND LOWER(v.label) = :size))))")
    Page<Product> searchProductsWithFilters(
        @Param("applyKeywordFilter") boolean applyKeywordFilter,
        @Param("keywordPattern") String keywordPattern,
        @Param("marque") String marque,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("inStock") Boolean inStock,
        @Param("size") String size,
        Pageable pageable
    );

    @Query("""
            SELECT COALESCE(v.attributeValue, v.label), COUNT(DISTINCT p.id)
            FROM Product p JOIN p.variants v
            WHERE p.deleted = FALSE
              AND v.attributeName IS NOT NULL
              AND (LOWER(v.attributeName) LIKE '%taille%'
                   OR LOWER(v.attributeName) LIKE '%size%'
                   OR LOWER(v.attributeName) LIKE '%pointure%')
              AND COALESCE(v.attributeValue, v.label) IS NOT NULL
            GROUP BY COALESCE(v.attributeValue, v.label)
            ORDER BY COUNT(DISTINCT p.id) DESC
            """)
    List<Object[]> countSizeFacets();

    @Query("""
            SELECT c.id, c.name, COUNT(p.id)
            FROM Product p JOIN p.category c
            WHERE p.deleted = FALSE
            GROUP BY c.id, c.name
            ORDER BY COUNT(p.id) DESC
            """)
    List<Object[]> countCategoryFacets();

    @Query("SELECT MIN(p.price), MAX(p.price), COUNT(p) FROM Product p WHERE p.deleted = FALSE")
    List<Object[]> priceBounds();
}
