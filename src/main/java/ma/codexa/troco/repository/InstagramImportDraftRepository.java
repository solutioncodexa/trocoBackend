package ma.codexa.troco.repository;

import ma.codexa.troco.entity.InstagramImportDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Toutes les requêtes portent explicitement le fournisseur : le service n'est pas transactionnel (appels réseau),
 * donc le filtre Hibernate multi-boutique n'y est pas actif.
 */
@Repository
public interface InstagramImportDraftRepository extends JpaRepository<InstagramImportDraft, Long> {

    long countByFournisseurIdAndStatus(Long fournisseurId, String status);

    List<InstagramImportDraft> findByFournisseurIdAndStatusOrderByCreatedAtDescIdDesc(Long fournisseurId, String status);

    /** Anti-doublon : un brouillon en attente ou déjà publié pour ce post. */
    Optional<InstagramImportDraft> findFirstByFournisseurIdAndSourceKeyAndStatusNotOrderByIdDesc(
            Long fournisseurId, String sourceKey, String status);

    Optional<InstagramImportDraft> findByIdAndFournisseurId(Long id, Long fournisseurId);

    List<InstagramImportDraft> findByFournisseurIdAndSourceTypeAndStatus(Long fournisseurId, String sourceType, String status);
}
