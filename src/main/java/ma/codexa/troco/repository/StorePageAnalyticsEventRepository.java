package ma.codexa.troco.repository;

import ma.codexa.troco.entity.StorePageAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StorePageAnalyticsEventRepository extends JpaRepository<StorePageAnalyticsEvent, Long> {

    @Query("""
            SELECT e.pageId, e.eventType, COUNT(e)
            FROM StorePageAnalyticsEvent e
            WHERE e.fournisseurId = :fid
              AND e.createdAt >= :from
            GROUP BY e.pageId, e.eventType
            """)
    List<Object[]> aggregateSince(@Param("fid") Long fid, @Param("from") LocalDateTime from);
}
