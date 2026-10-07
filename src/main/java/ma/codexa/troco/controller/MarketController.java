package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.entity.OrderReturn;
import ma.codexa.troco.entity.ReferralCode;
import ma.codexa.troco.entity.SeasonalCampaign;
import ma.codexa.troco.entity.ShippingCityRate;
import ma.codexa.troco.market.MarketService;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/market/public/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicConfig() {
        return ResponseEntity.ok(ApiResponse.success(marketService.publicConfig()));
    }

    @PutMapping("/market/config")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConfig(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(marketService.updateConfig(body)));
    }

    @GetMapping("/market/email-templates")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<Map<String, Object>>> emailTemplates() {
        return ResponseEntity.ok(ApiResponse.success(marketService.emailTemplates()));
    }

    @PutMapping("/market/email-templates")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateEmailTemplates(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(marketService.updateEmailTemplates(body)));
    }

    @GetMapping("/market/city-rates")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<List<ShippingCityRate>>> cityRates() {
        return ResponseEntity.ok(ApiResponse.success(marketService.listCityRates()));
    }

    @PutMapping("/market/city-rates")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<ShippingCityRate>> upsertCityRate(@RequestBody Map<String, Object> body) {
        BigDecimal fee = body.get("fee") == null ? null : new BigDecimal(String.valueOf(body.get("fee")));
        return ResponseEntity.ok(ApiResponse.success(marketService.upsertCityRate(
                str(body.get("carrierCode")), str(body.get("city")), fee)));
    }

    @DeleteMapping("/market/city-rates/{id}")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<Void> deleteCityRate(@PathVariable Long id) {
        marketService.deleteCityRate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/market/campaigns")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<List<SeasonalCampaign>>> campaigns() {
        return ResponseEntity.ok(ApiResponse.success(marketService.listCampaigns()));
    }

    @PutMapping("/market/campaigns")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<SeasonalCampaign>> saveCampaign(@RequestBody Map<String, Object> body) {
        SeasonalCampaign c = new SeasonalCampaign();
        c.setCode(str(body.get("code")));
        c.setTitle(str(body.get("title")));
        if (body.get("discountPercent") != null) {
            c.setDiscountPercent(new BigDecimal(String.valueOf(body.get("discountPercent"))));
        }
        if (body.get("startsOn") != null && !String.valueOf(body.get("startsOn")).isBlank()) {
            c.setStartsOn(LocalDate.parse(String.valueOf(body.get("startsOn"))));
        }
        if (body.get("endsOn") != null && !String.valueOf(body.get("endsOn")).isBlank()) {
            c.setEndsOn(LocalDate.parse(String.valueOf(body.get("endsOn"))));
        }
        c.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.ok(ApiResponse.success(marketService.saveCampaign(c)));
    }

    @GetMapping("/market/referrals")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<List<ReferralCode>>> referrals() {
        return ResponseEntity.ok(ApiResponse.success(marketService.listReferrals()));
    }

    @PutMapping("/market/referrals")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<ReferralCode>> saveReferral(@RequestBody Map<String, Object> body) {
        BigDecimal reward = body.get("rewardMad") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("rewardMad")));
        return ResponseEntity.ok(ApiResponse.success(
                marketService.saveReferral(str(body.get("code")), reward, str(body.get("referrerLabel")))));
    }

    @GetMapping("/market/public/referral")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewReferral(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(marketService.previewReferral(code)));
    }

    @PostMapping("/market/public/returns")
    public ResponseEntity<ApiResponse<OrderReturn>> requestReturn(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(marketService.requestReturn(
                str(body.get("orderNumber")), str(body.get("phone")), str(body.get("reason")))));
    }

    @GetMapping("/market/returns")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<List<OrderReturn>>> returns() {
        return ResponseEntity.ok(ApiResponse.success(marketService.listReturns()));
    }

    @PatchMapping("/market/returns/{id}")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<OrderReturn>> updateReturn(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(marketService.updateReturn(id, body.get("status"))));
    }

    @GetMapping(value = "/market/orders.csv", produces = "text/csv")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<String> ordersCsv() {
        return csv(marketService.ordersCsv(), "commandes.csv");
    }

    @GetMapping(value = "/catalog/meta.csv", produces = "text/csv")
    public ResponseEntity<String> metaCatalog() {
        return csv(marketService.metaCatalogCsv(), "meta-catalog.csv");
    }

    private static ResponseEntity<String> csv(String body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }

    private static String str(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }
}
