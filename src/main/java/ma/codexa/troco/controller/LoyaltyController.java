package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/public/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balance(@RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.balance(phone)));
    }
}
