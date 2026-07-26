package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.ShippingCarrierDTO;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import ma.codexa.troco.service.ShippingCarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shipping-carriers")
@RequiredArgsConstructor
public class ShippingCarrierController {

    private final ShippingCarrierService shippingCarrierService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ShippingCarrierDTO>>> listPublic(
            @RequestParam(required = false) BigDecimal subtotal) {
        return ResponseEntity.ok(ApiResponse.success(shippingCarrierService.listPublic(subtotal)));
    }

    @GetMapping("/public/quote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quote(
            @RequestParam String carrierCode,
            @RequestParam BigDecimal subtotal) {
        BigDecimal fee = shippingCarrierService.quote(carrierCode, subtotal);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "carrierCode", carrierCode,
                "subtotal", subtotal,
                "shippingFee", fee
        )));
    }

    @GetMapping
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<List<ShippingCarrierDTO>>> listAdmin() {
        return ResponseEntity.ok(ApiResponse.success(shippingCarrierService.listAdmin()));
    }

    @PutMapping
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<ShippingCarrierDTO>> upsert(@RequestBody ShippingCarrierDTO body) {
        return ResponseEntity.ok(ApiResponse.success(shippingCarrierService.upsert(body)));
    }
}
