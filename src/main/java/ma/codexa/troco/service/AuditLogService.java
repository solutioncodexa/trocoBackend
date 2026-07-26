package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.AuditLogDTO;
import ma.codexa.troco.entity.AuditEvent;
import ma.codexa.troco.entity.User;
import ma.codexa.troco.repository.AuditEventRepository;
import ma.codexa.troco.security.UserDetailsImpl;
import ma.codexa.troco.security.service.PermissionCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Dual-write audit: structured file logger + DB {@link AuditEvent} for admin UI.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    public enum Outcome { SUCCESS, FAILURE, DENIED }

    public enum Action {
        LOGIN, LOGOUT, REGISTER, REFRESH_TOKEN, PASSWORD_CHANGE,
        ADMIN_LOGIN, ADMIN_CREATE, ADMIN_UPDATE, ADMIN_DELETE,
        PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
        CATEGORY_CREATE, CATEGORY_UPDATE, CATEGORY_DELETE,
        COLLECTION_CREATE, COLLECTION_UPDATE, COLLECTION_DELETE,
        FEATURED_PRODUCT_CHANGE,
        ORDER_CREATE, ORDER_UPDATE_STATUS, ORDER_DELETE,
        CUSTOM_ORDER_SUBMIT, CUSTOM_ORDER_UPDATE,
        STOCK_ADJUST,
        MEMBER_CREATE, MEMBER_UPDATE, MEMBER_ACTIVATE, MEMBER_DEACTIVATE, MEMBER_PASSWORD_RESET, MEMBER_DELETE,
        FILE_UPLOAD, FILE_DELETE,
        UNAUTHORIZED_ACCESS, RATE_LIMIT_EXCEEDED,
        TOP_BAR_MESSAGE_UPDATE, PROMO_MODAL_UPDATE,
        PAGE_CREATE, PAGE_UPDATE, PAGE_PUBLISH, PAGE_UNPUBLISH, PAGE_DELETE,
        PAGE_BLOCKS_UPDATE, PAGE_AB_PROMOTE, WEBHOOK_UPDATE
    }

    private static final Logger AUDIT = LoggerFactory.getLogger("troco.audit");

    private final AuditEventRepository auditEventRepository;
    private final PermissionCheckService permissionCheckService;

    public void log(Action action, Outcome outcome, String target) {
        log(action, outcome, target, null, null);
    }

    public void log(Action action, Outcome outcome, String target, String details) {
        log(action, outcome, target, details, null);
    }

    public void log(Action action, Outcome outcome, String target, String details,
                    Map<String, Object> extras) {
        writeFile(action, outcome, target, details, extras);
        if (outcome == Outcome.SUCCESS) {
            persistDb(action.name(), inferEntity(action), target, details);
        }
    }

    @Transactional
    public void record(Action action, String entityName, String entityId, String description) {
        writeFile(action, Outcome.SUCCESS, entityId, description, null);
        persistDb(action.name(), entityName, entityId, description);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> search(Long userId, String action, String entity, Pageable pageable) {
        String a = blankToNull(action);
        String e = blankToNull(entity);
        return auditEventRepository.search(userId, a, e, pageable).map(this::toDto);
    }

    private void persistDb(String action, String entityName, String entityId, String description) {
        try {
            Long userId = null;
            String username = null;
            String fullName = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl udi) {
                userId = udi.getId();
                username = udi.getUsername();
                fullName = udi.getFullName();
            } else {
                User u = permissionCheckService.currentUser().orElse(null);
                if (u != null) {
                    userId = u.getId();
                    username = u.getEmail();
                    fullName = u.getFullName();
                } else if (auth != null) {
                    username = auth.getName();
                }
            }
            String safeEntityId = entityId;
            if (safeEntityId != null && safeEntityId.length() > 80) {
                safeEntityId = safeEntityId.substring(0, 80);
            }
            auditEventRepository.save(AuditEvent.builder()
                    .userId(userId)
                    .username(username)
                    .userFullName(fullName)
                    .action(action)
                    .entityName(entityName)
                    .entityId(safeEntityId)
                    .description(description)
                    .build());
        } catch (Exception ex) {
            AUDIT.warn("AUDIT_DB_WRITE_FAILED action={} err={}", action, ex.getMessage());
        }
    }

    private void writeFile(Action action, Outcome outcome, String target, String details,
                           Map<String, Object> extras) {
        try {
            MDC.put("auditAction", action.name());
            MDC.put("auditTarget", target == null ? "" : target);
            MDC.put("auditOutcome", outcome.name());
            if (extras != null) {
                extras.forEach((k, v) -> MDC.put("audit." + k, String.valueOf(v)));
            }
            String message = "AUDIT %s %s target=%s%s".formatted(
                    action.name(),
                    outcome.name(),
                    target,
                    details != null ? " details=\"" + details + "\"" : "");
            switch (outcome) {
                case SUCCESS -> AUDIT.info(message);
                case FAILURE, DENIED -> AUDIT.warn(message);
            }
        } finally {
            MDC.remove("auditAction");
            MDC.remove("auditTarget");
            MDC.remove("auditOutcome");
            if (extras != null) {
                extras.keySet().forEach(k -> MDC.remove("audit." + k));
            }
        }
    }

    private AuditLogDTO toDto(AuditEvent e) {
        return AuditLogDTO.builder()
                .id(e.getId())
                .action(e.getAction())
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .userId(e.getUserId())
                .username(e.getUsername())
                .userFullName(e.getUserFullName())
                .build();
    }

    private static String inferEntity(Action action) {
        String n = action.name();
        if (n.startsWith("PRODUCT")) return "PRODUCT";
        if (n.startsWith("ORDER")) return "ORDER";
        if (n.startsWith("CUSTOM_ORDER")) return "CUSTOM_ORDER";
        if (n.startsWith("STOCK")) return "STOCK";
        if (n.startsWith("MEMBER") || n.startsWith("ADMIN")) return "USER";
        if (n.startsWith("CATEGORY")) return "CATEGORY";
        if (n.startsWith("COLLECTION")) return "COLLECTION";
        if (n.startsWith("PAGE")) return "STORE_PAGE";
        if (n.startsWith("WEBHOOK")) return "WEBHOOK";
        if (n.contains("LOGIN") || n.contains("LOGOUT") || n.equals("REGISTER") || n.equals("REFRESH_TOKEN")) {
            return "AUTH";
        }
        return "OTHER";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
