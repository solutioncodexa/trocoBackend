package ma.codexa.troco.repository;

import ma.codexa.troco.entity.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {
    Optional<Fournisseur> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);

    @Query("SELECT f FROM Fournisseur f WHERE LOWER(f.customDomain) = LOWER(:domain)")
    Optional<Fournisseur> findByCustomDomainIgnoreCase(@Param("domain") String domain);

    boolean existsByCustomDomainIgnoreCase(String customDomain);

    List<Fournisseur> findAllByOrderByCreatedAtDesc();
}
