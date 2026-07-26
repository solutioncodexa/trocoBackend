package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StorePageVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorePageVersionRepository extends JpaRepository<StorePageVersion, Long> {
    List<StorePageVersion> findByPageIdOrderByCreatedAtDesc(Long pageId);

    long countByPageId(Long pageId);
}
