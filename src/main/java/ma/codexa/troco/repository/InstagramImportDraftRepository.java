package ma.codexa.troco.repository;

import ma.codexa.troco.entity.InstagramImportDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstagramImportDraftRepository extends JpaRepository<InstagramImportDraft, Long> {

    List<InstagramImportDraft> findByStatusOrderByCreatedAtDescIdDesc(String status);

    /** Anti-doublon : un brouillon en attente ou déjà publié pour ce post. */
    Optional<InstagramImportDraft> findFirstBySourceKeyAndStatusNotOrderByIdDesc(String sourceKey, String status);
}
