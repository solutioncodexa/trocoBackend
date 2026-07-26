package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StorePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorePageRepository extends JpaRepository<StorePage, Long> {

    List<StorePage> findAllByOrderBySortOrderAscTitleAsc();

    Optional<StorePage> findBySlugIgnoreCase(String slug);

    Optional<StorePage> findByPreviewToken(String previewToken);

    Optional<StorePage> findFirstByIsHomeTrueAndPublishedTrue();

    List<StorePage> findByIsHomeTrueAndPublishedTrueOrderByAbVariantAscIdAsc();

    List<StorePage> findByPublishedTrueAndShowInNavTrueOrderBySortOrderAscTitleAsc();

    List<StorePage> findByPublishedTrueOrderBySortOrderAscTitleAsc();

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    boolean existsBySlugIgnoreCase(String slug);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StorePage p SET p.isHome = false WHERE p.fournisseurId = :fid AND p.isHome = true")
    void clearHomeFlags(@Param("fid") Long fid);

    /** Retire le flag home des pages sans variante A/B, ou de la même variante. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StorePage p SET p.isHome = false
            WHERE p.fournisseurId = :fid AND p.isHome = true
              AND (p.abVariant IS NULL OR LOWER(p.abVariant) = LOWER(:variant))
            """)
    void clearHomeFlagsForVariant(@Param("fid") Long fid, @Param("variant") String variant);
}