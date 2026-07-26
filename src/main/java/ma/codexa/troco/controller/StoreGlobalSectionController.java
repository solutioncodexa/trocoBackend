package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.StoreGlobalSectionDTO;
import ma.codexa.troco.dto.request.UpsertGlobalSectionRequest;
import ma.codexa.troco.service.StoreGlobalSectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-global-sections")
@RequiredArgsConstructor
public class StoreGlobalSectionController {

    private final StoreGlobalSectionService service;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<StoreGlobalSectionDTO>>> listPublic() {
        return ResponseEntity.ok(ApiResponse.success(service.listPublicEnabled()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StoreGlobalSectionDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.listAdmin()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreGlobalSectionDTO>> upsert(@Valid @RequestBody UpsertGlobalSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.upsert(request), "Section enregistrée"));
    }
}
