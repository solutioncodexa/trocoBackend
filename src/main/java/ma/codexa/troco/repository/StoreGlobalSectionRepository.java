package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StoreGlobalSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreGlobalSectionRepository extends JpaRepository<StoreGlobalSection, Long> {
    Optional<StoreGlobalSection> findBySectionKey(String sectionKey);

    List<StoreGlobalSection> findAllByOrderBySectionKeyAsc();

    List<StoreGlobalSection> findByEnabledTrue();
}
