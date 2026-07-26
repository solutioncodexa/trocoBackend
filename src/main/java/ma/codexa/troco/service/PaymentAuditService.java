package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.PaymentAuditLog;
import ma.codexa.troco.repository.PaymentAuditLogRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentAuditLogRepository paymentAuditLogRepository;

    @Transactional
    public void record(Long orderId, String orderNumber, String provider, String eventType,
                       BigDecimal amount, String currency, String status, String payloadJson) {
        PaymentAuditLog log = new PaymentAuditLog();
        log.setFournisseurId(TenantContext.getFournisseurId());
        log.setOrderId(orderId);
        log.setOrderNumber(orderNumber);
        log.setProvider(provider);
        log.setEventType(eventType);
        log.setAmount(amount);
        log.setCurrency(currency);
        log.setStatus(status);
        log.setPayloadJson(payloadJson);
        paymentAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<PaymentAuditLog> list(Pageable pageable) {
        return paymentAuditLogRepository.findByFournisseurIdOrderByCreatedAtDesc(
                TenantContext.requireFournisseurId(), pageable);
    }
}
