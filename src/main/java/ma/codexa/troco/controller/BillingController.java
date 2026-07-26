package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.BillingResultDTO;
import ma.codexa.troco.dto.CmiCheckoutDTO;
import ma.codexa.troco.service.CmiBillingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final CmiBillingService cmiBillingService;

    /**
     * Paiement simulé (tests / démo) — aucune redirection CMI.
     * Contrôlé par {@code app.cmi.test-pass=true}.
     */
    @PostMapping("/test-pass")
    public ResponseEntity<ApiResponse<BillingResultDTO>> testPass(@RequestBody(required = false) Map<String, String> body) {
        String planCode = body != null ? body.get("planCode") : null;
        return ResponseEntity.ok(ApiResponse.success(cmiBillingService.simulateTestPayment(planCode)));
    }

    /** Démarre un paiement CMI réel pour le plan choisi (admin boutique). */
    @PostMapping("/cmi/checkout")
    public ResponseEntity<ApiResponse<CmiCheckoutDTO>> checkout(@RequestBody Map<String, String> body) {
        String planCode = body != null ? body.get("planCode") : null;
        return ResponseEntity.ok(ApiResponse.success(cmiBillingService.initiatePlanCheckout(planCode)));
    }

    /** Callback serveur-à-serveur CMI — réponse texte APPROVED/DECLINED. */
    @PostMapping(value = "/cmi/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> callback(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(cmiBillingService.handleCallback(params));
    }

    /** Retour navigateur OK (optionnel — le callback fait foi). */
    @PostMapping(value = "/cmi/ok", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> ok(@RequestParam Map<String, String> params) {
        cmiBillingService.markReturnPaidIfApproved(params);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "ok",
                "oid", params.getOrDefault("oid", params.getOrDefault("ReturnOid", ""))
        )));
    }

    @PostMapping(value = "/cmi/fail", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> fail(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "fail",
                "oid", params.getOrDefault("oid", params.getOrDefault("ReturnOid", ""))
        )));
    }
}
