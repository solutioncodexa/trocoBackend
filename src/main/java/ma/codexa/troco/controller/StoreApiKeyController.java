package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.AuditLogService;
import ma.codexa.troco.service.StoreApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class StoreApiKeyController {

    private final StoreApiKeyService storeApiKeyService;
    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission(AppPermissions.API_KEYS_MANAGE)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        return ResponseEntity.ok(ApiResponse.success(storeApiKeyService.list()));
    }

    @PostMapping
    @RequirePermission(AppPermissions.API_KEYS_MANAGE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, String> body) {
        Map<String, Object> created = storeApiKeyService.create(body.get("name"), body.get("scopes"));
        auditLogService.log(AuditLogService.Action.API_KEY_CREATE, AuditLogService.Outcome.SUCCESS,
                String.valueOf(created.get("id")), "API key created");
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(AppPermissions.API_KEYS_MANAGE)
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        storeApiKeyService.revoke(id);
        auditLogService.log(AuditLogService.Action.API_KEY_REVOKE, AuditLogService.Outcome.SUCCESS,
                String.valueOf(id), "API key revoked");
        return ResponseEntity.noContent().build();
    }
}
