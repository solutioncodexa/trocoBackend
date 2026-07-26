package ma.codexa.troco.repository;

import ma.codexa.troco.entity.AbandonedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AbandonedCartRepository extends JpaRepository<AbandonedCart, Long> {

    Optional<AbandonedCart> findBySessionKey(String sessionKey);

    Optional<AbandonedCart> findByRecoveryToken(String recoveryToken);

    List<AbandonedCart> findAllByOrderByUpdatedAtDesc();

    @Query("""
            SELECT c FROM AbandonedCart c
            WHERE c.reminderSent = false AND c.recovered = false
              AND c.remindAt IS NOT NULL AND c.remindAt <= :now
              AND (c.customerEmail IS NOT NULL OR c.customerPhone IS NOT NULL)
            """)
    List<AbandonedCart> findDueReminders(@Param("now") LocalDateTime now);
}
