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
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithImages(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    // Filtrer par style
    List<Product> findByStyle(String style);

    // Filtrer par type d'or
    List<Product> findByGoldType(String goldType);

    // Filtrer par type de produit
    List<Product> findByProductType(String productType);

    // Filtrer par catégorie
    List<Product> findByCategoryId(Long categoryId);

    // Filtrer par plage de prix
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // Produits en stock
    @Query("SELECT p FROM Product p WHERE p.stock > 0")
    List<Product> findInStockProducts();

    // Recherche par nom (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Filtres combinés (avec images pour le mapping DTO)
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT p FROM Product p WHERE " +
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
}
