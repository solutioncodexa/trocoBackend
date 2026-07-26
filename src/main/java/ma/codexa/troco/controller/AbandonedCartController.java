package ma.codexa.troco.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.AbandonedCartDTO;
import ma.codexa.troco.dto.AbandonedCartListItemDTO;
import ma.codexa.troco.dto.request.CaptureAbandonedCartRequest;
import ma.codexa.troco.service.AbandonedCartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/abandoned-carts")
@RequiredArgsConstructor
public class AbandonedCartController {

    private final AbandonedCartService abandonedCartService;

    @PostMapping("/public/capture")
    public ResponseEntity<ApiResponse<AbandonedCartDTO>> capture(@Valid @RequestBody CaptureAbandonedCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(abandonedCartService.capture(request)));
    }

    @GetMapping("/public/recover/{token}")
    public ResponseEntity<ApiResponse<AbandonedCartDTO>> recover(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(abandonedCartService.recover(token)));
    }

    @PostMapping("/public/recovered")
    public ResponseEntity<ApiResponse<Void>> markRecovered(@RequestBody Map<String, String> body) {
        abandonedCartService.markRecovered(body != null ? body.get("sessionKey") : null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<AbandonedCartListItemDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(abandonedCartService.listAdmin()));
    }
}
