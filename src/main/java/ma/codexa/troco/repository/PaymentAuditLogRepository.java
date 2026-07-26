package ma.codexa.troco.repository;

import ma.codexa.troco.entity.PaymentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {
    Page<PaymentAuditLog> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId, Pageable pageable);
}
