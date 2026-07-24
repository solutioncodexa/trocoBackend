package ma.codexa.troco.repository;

import ma.codexa.troco.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            SELECT a FROM AuditEvent a
            WHERE (:userId IS NULL OR a.userId = :userId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entity IS NULL OR a.entityName = :entity)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditEvent> search(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entity") String entity,
            Pageable pageable);
}
