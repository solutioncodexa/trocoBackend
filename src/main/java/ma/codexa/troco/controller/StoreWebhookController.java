package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.StoreWebhookDTO;
import ma.codexa.troco.dto.StoreWebhookDeliveryDTO;
import ma.codexa.troco.dto.StoreWebhookListItemDTO;
import ma.codexa.troco.dto.request.UpsertStoreWebhookRequest;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.StoreWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-webhooks")
@RequiredArgsConstructor
public class StoreWebhookController {

    private final StoreWebhookService storeWebhookService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @RequirePermission(AppPermissions.WEBHOOKS_MANAGE)
    public ResponseEntity<ApiResponse<List<StoreWebhookListItemDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(storeWebhookService.list()));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @RequirePermission(AppPermissions.WEBHOOKS_MANAGE)
    public ResponseEntity<ApiResponse<List<StoreWebhookDeliveryDTO>>> deliveries() {
        return ResponseEntity.ok(ApiResponse.success(storeWebhookService.recentDeliveries()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @RequirePermission(AppPermissions.WEBHOOKS_MANAGE)
    public ResponseEntity<ApiResponse<StoreWebhookDTO>> create(@Valid @RequestBody UpsertStoreWebhookRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeWebhookService.create(request), "Webhook créé"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @RequirePermission(AppPermissions.WEBHOOKS_MANAGE)
    public ResponseEntity<ApiResponse<StoreWebhookListItemDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertStoreWebhookRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeWebhookService.update(id, request), "Webhook mis à jour"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @RequirePermission(AppPermissions.WEBHOOKS_MANAGE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storeWebhookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
