package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StorePageBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorePageBlockRepository extends JpaRepository<StorePageBlock, Long> {
    List<StorePageBlock> findByPageIdOrderBySortOrderAsc(Long pageId);

    long countByPageId(Long pageId);
    void deleteByPageId(Long pageId);
}
