package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.entity.PaymentAuditLog;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.PaymentAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment-audit")
@RequiredArgsConstructor
public class PaymentAuditController {

    private final PaymentAuditService paymentAuditService;

    @GetMapping
    @RequirePermission(AppPermissions.AUDIT_VIEW)
    public ResponseEntity<ApiResponse<PageResponse<PaymentAuditLog>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PaymentAuditLog> p = paymentAuditService.list(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements())));
    }
}
