package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.StoreLeadDTO;
import ma.codexa.troco.dto.StoreLeadListItemDTO;
import ma.codexa.troco.dto.request.CreateStoreLeadRequest;
import ma.codexa.troco.service.StoreLeadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/store-leads")
@RequiredArgsConstructor
public class StoreLeadController {

    private final StoreLeadService storeLeadService;

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<StoreLeadDTO>> submit(@Valid @RequestBody CreateStoreLeadRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeLeadService.submit(request), "Message reçu"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<StoreLeadListItemDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(storeLeadService.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<StoreLeadDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(storeLeadService.get(id)));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] body = storeLeadService.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
