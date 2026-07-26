package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    @GetMapping("/export")
    @RequirePermission(AppPermissions.PRIVACY_MANAGE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> export(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(ApiResponse.success(privacyService.exportSubject(email, phone)));
    }

    @PostMapping("/erase")
    @RequirePermission(AppPermissions.PRIVACY_MANAGE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> erase(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                privacyService.eraseSubject(body.get("email"), body.get("phone"))));
    }
}
